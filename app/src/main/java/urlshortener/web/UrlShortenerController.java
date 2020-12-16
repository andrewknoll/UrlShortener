package urlshortener.web;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.http.CacheControl;
import org.apache.http.client.ClientProtocolException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import urlshortener.domain.QR;
import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.QRService;
import urlshortener.service.SafeCheckService;
import urlshortener.service.ShortURLService;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static urlshortener.eip.Router.QR_URI;
import org.apache.camel.ProducerTemplate;

@RestController
public class UrlShortenerController {

  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final QRService qrService;

  private final ProducerTemplate producerTemplate;

  private final SafeCheckService safeCheckService;

  private final String defaultFormat = "png";

  // Path to sponsor.html file
  @Value("classpath:static/sponsor.html")
  Resource sponsorResource;

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService, QRService qrService,
      SafeCheckService safeCheckService, ProducerTemplate producerTemplate) throws IOException {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
    this.qrService = qrService;
    this.safeCheckService = safeCheckService;
    this.producerTemplate = producerTemplate;
  }

  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id, HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      // If not safe return bad request
      String finalURL = l.getTarget();
      System.out.println("Uri is safe " + l.getSafe().toString() );
      if (l.getSafe() && this.reachableURL(finalURL)) {
        clickService.saveClick(id, extractIP(request));
        return redirectThroughSponsor();
      } else if (!l.getSafe()){
        String json = Json.createObjectBuilder().add("error", l.getDescription()).build().toString();
        return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
      }
      else {
        String json = Json.createObjectBuilder().add("error", "URI not reachable").build().toString();
        return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
      }
    } else {
      return error("Provided hash has not been found or is not valid", HttpStatus.NOT_FOUND);
    }
  }

  /**
   * We can recover final URI (in JSON format) from hash thanks to this GET method
   */
  @RequestMapping(value= "/redirect/{hash:(?!link|index).*}", method = RequestMethod.GET, produces = "application/json")
  public ResponseEntity<?>getFinalURI(@PathVariable String hash, HttpServletRequest request){
    ShortURL l = shortUrlService.findByKey(hash);
    if (l != null) {
      String json = Json.createObjectBuilder().add("URI", l.getTarget()).build().toString();
      return new ResponseEntity<>(json, HttpStatus.ACCEPTED);
    }
    else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/link", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> shortener(@RequestParam("url") String url,
      @RequestParam(value = "sponsor", required = false) String sponsor,
      @RequestParam(value = "generateQR", defaultValue = "false") boolean generateQR, HttpServletRequest request)
      throws ClientProtocolException, IOException, URISyntaxException {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" }, UrlValidator.ALLOW_2_SLASHES);

    if (urlValidator.isValid(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr(), generateQR);
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());
      HttpStatus status = HttpStatus.CREATED;
      try {
        if (generateQR) {
          qrService.save(su);
        }
      } catch (Exception e) {
        status = HttpStatus.PARTIAL_CONTENT;
      }

      safeBrowsingCheck(su, url);

      return new ResponseEntity<>(su, h, status);

    } else {
      String json = Json.createObjectBuilder().add("error", "debe ser una URI http o https").build().toString();
      return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
    }

  }

  @RequestMapping(value = { "/qr/{hash}", "qr/{hash}.{format}" }, method = RequestMethod.GET)
  public ResponseEntity<?> retrieveQRCodebyHash(@PathVariable String hash,
      @PathVariable(required = false) String format, HttpServletRequest request) throws URISyntaxException {
    if (defaultFormat.equals(format) || format == null) {
      QR q = qrService.findByHash(hash); //Try to find if QR was already generated
      ShortURL su = shortUrlService.findByKey(hash); //Try to find ShortUrl
      if (su != null) {
        clickService.saveClick(hash, extractIP(request));
        shortUrlService.updateShortUrl(su, new URI(extractIP(request) + "/" + su.getHash()), su.getSafe(), su.getDescription());
        if (q == null) { //if QR was never generated
          return producerTemplate.requestBody(QR_URI, hash, ResponseEntity.class);
        } else {
          HttpHeaders h = new HttpHeaders();
          h.add("hash", hash);
          h.setLocation(q.getUri());
          h.setCacheControl(cacheConfig());
          return new ResponseEntity<byte[]>(q.getQR(), h, HttpStatus.ACCEPTED);
        }
      }
    }
    return error("Provided hash has not been found or is not valid", HttpStatus.NOT_FOUND);
  }

  @RequestMapping(value = "/multipleLinks", method = RequestMethod.POST, produces = "text/csv")
  public ResponseEntity<String> multipleShortener(@RequestParam("urls") MultipartFile urls,
      HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });

    try {
      System.out.println("Received type " + urls.getContentType());
      BufferedReader br;

      List<String> urlsList = new ArrayList<>();
      List<String> problems = new ArrayList<>();

      String line;
      InputStream is = urls.getInputStream();
      br = new BufferedReader(new InputStreamReader(is));
      while ((line = br.readLine()) != null) {
        urlsList.add(line);
      }
      // Check if all urls are valid and safe
      for (int i = 0; i < urlsList.size(); i++) {
        if (!urlValidator.isValid(urlsList.get(i))) {
          problems.add(i, "debe ser una URI http o https");
        } else {
          problems.add(i, "");
        }
      }

      List<String> resultList = new ArrayList<>();
      ShortURL firstSu = new ShortURL();

      for (int i = 0; i < urlsList.size(); i++) {
        if (problems.get(i).isEmpty()) {
          ShortURL su = shortUrlService.save(urlsList.get(i), "", request.getRemoteAddr(), false);
          // Save the first URL
          if (resultList.size() == 1) {
            firstSu = su;
          }
          resultList.add(su.getUri().toString());



          // Async process to check if URLs are safe
          safeBrowsingCheck(su, su.getTarget());

        } else {
          resultList.add("");
        }

      }

      // FileWriter fileWriter = new FileWriter("csvResponse.csv");
      String resultString = "data:text/csv;charset=utf-8,";

      for (int i = 0; i < resultList.size(); i++) {
        resultString = resultString + (urlsList.get(i) + ',' + resultList.get(i) + "," + problems.get(i) + "\n");
      }

      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setLocation(firstSu.getUri());

      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("Location", resultList.get(0));
//TODO:Header que descarga automáticamente (ContentDisposition)
      return new ResponseEntity<>(resultString, headers, HttpStatus.CREATED);

    } catch (Exception e) {
      System.out.println("Exception  " + e.toString());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public void safeBrowsingCheck(ShortURL su, String url){
    googleSafeBrowsing(su, url, safeCheckService, shortUrlService);
  }

  public static void googleSafeBrowsing(ShortURL su, String url, SafeCheckService safeCheckService, ShortURLService shortUrlService) {
    try {
      safeCheckService.safeBrowsingChecker(url).thenAcceptAsync((result) -> {

        if (result.get(0).equals("SAFE")) {
          shortUrlService.updateShortUrl(su, su.getUri(), true, "");
        } else {
          shortUrlService.updateShortUrl(su, su.getUri(), false, result.get(1));
        }
      }).exceptionally (e -> {
        shortUrlService.updateShortUrl(su, su.getUri(), false, "No se ha podido verificar con google Safe Browsing");
        return null;
      });
    } catch (IOException e) {
      shortUrlService.updateShortUrl(su, su.getUri(), false, "No se ha podido verificar con google Safe Browsing");
      e.printStackTrace();
    }

  }

  /**
   * Function that shows sponsor.html page before redirecting to final URI
   */
  private ResponseEntity<?> redirectThroughSponsor() {
    try {
      // Reading HTML file
      File resource = sponsorResource.getFile();
      // Data = html string
      String data = new String(
              Files.readAllBytes(resource.toPath()));
      // Shows sponsor.html page without changing location
      return new ResponseEntity<>(data, HttpStatus.TEMPORARY_REDIRECT);

    }catch (IOException e) {
      e.printStackTrace();
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);

    }
  }

  private String extractIP(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  /**
   * 
   * @param inputURL URL to being shortened
   * @return True, if and only if, inputURL is reachable.
   */
  private boolean reachableURL(String inputURL) {
    HttpURLConnection httpURLConn;
    try {
      httpURLConn = (HttpURLConnection) new URL(inputURL).openConnection();
      // HEAD request is like GET request but just expecting headers, not resources
      httpURLConn.setRequestMethod("HEAD");
      // System.out.println("Status request: " + httpURLConn.getResponseCode())
      return httpURLConn.getResponseCode() == HttpURLConnection.HTTP_OK;
    } catch (Exception e) {
      System.out.println("Error: " + e.getMessage());
      return false;
    }
  }
  
  private ResponseEntity<?> error(String message, HttpStatus status) {
    HashMap<String, String> map = new HashMap<>();
    map.put("error", message);
    return new ResponseEntity<>(map, status);
  }

  private CacheControl cacheConfig() {
    return CacheControl.maxAge(10, TimeUnit.DAYS).cachePublic();
  }
}
