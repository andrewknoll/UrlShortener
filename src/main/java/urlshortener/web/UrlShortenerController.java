package urlshortener.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import org.springframework.http.MediaType;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.validator.routines.UrlValidator;
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

@RestController
public class UrlShortenerController {
  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final QRService qrService;

  private final SafeCheckService safeCheckService;

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService, QRService qrService, SafeCheckService safeCheckService) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
    this.qrService = qrService;
    this.safeCheckService = safeCheckService;
  }

  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id, HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      // If not safe return bad request
      if (l.getSafe()) {
        clickService.saveClick(id, extractIP(request));
        return createSuccessfulRedirectToResponse(l);
      } else {
        String json = Json.createObjectBuilder().add("error", l.getDescription()).build().toString();
        return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
      }
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/qr/{hash:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<byte[]> retrieveQRCodebyHash(@PathVariable String hash,
      HttpServletRequest request) {
    QR q = qrService.findByHash(hash);
    ShortURL su = shortUrlService.findByKey(hash);
    if (q != null && su != null) {
      clickService.saveClick(hash, extractIP(request));
      HttpHeaders h = new HttpHeaders();
      h.setLocation(URI.create(su.getTarget()));
    return new ResponseEntity<>(q.getQR(), h, HttpStatus.ACCEPTED);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/file/{filename:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<byte[]> retrieveQRCodebyName(@PathVariable String filename,
      HttpServletRequest request) {
    QR q = qrService.findByName(filename);
    ShortURL su = null;
    if (q != null) su = shortUrlService.findByKey(q.getHash());
    if (q != null && su != null) {
      clickService.saveClick(q.getHash(), extractIP(request));
      HttpHeaders h = new HttpHeaders();
      h.setLocation(URI.create(su.getTarget()));
    return new ResponseEntity<>(q.getQR(), h, HttpStatus.ACCEPTED);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
  
  @RequestMapping(value = "/qr", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> generateQRCode(@RequestParam("hash") String hash,
                                     @RequestParam(value = "filename", required = false) String fileName,
                                     HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(hash);
    if (l != null) {
      QR qr = qrService.save(l, fileName);
      HttpHeaders h = new HttpHeaders();
      h.setLocation(qr.getUri());
      h.add("hash", qr.getHash());
      h.add("filename", qr.getFileName());
      return new ResponseEntity<>(qr.getQR(), h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<?> shortener(@RequestParam("url") String url,
      @RequestParam(value = "sponsor", required = false) String sponsor, HttpServletRequest request)
      throws ClientProtocolException, IOException {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" }, UrlValidator.ALLOW_2_SLASHES);

    if (urlValidator.isValid(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());

      safeBrowsingCheck(su, url);

      return new ResponseEntity<>(su, h, HttpStatus.CREATED);

    } else {
      String json = Json.createObjectBuilder().add("error", "debe ser una URI http o https").build().toString();
      return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
    }

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
          ShortURL su = shortUrlService.save(urlsList.get(i), "", request.getRemoteAddr());
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

      return new ResponseEntity<>(resultString, headers, HttpStatus.CREATED);

    } catch (Exception e) {
      System.out.println("Exception  " + e.toString());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public void safeBrowsingCheck(ShortURL su, String url){
    try {
      safeCheckService.safeBrowsingChecker(url).thenAcceptAsync((result) -> {
        if (result.get(0).equals("SAFE")) {
          shortUrlService.updateShortUrl(su, true, "");
        } else {
          shortUrlService.updateShortUrl(su, false, result.get(1));
        }
      });
    } catch (Exception e) {
      shortUrlService.updateShortUrl(su, false, "No se ha podido verificar con google Safe Browsing");
      System.out.println("Exception in thread");
    }
  }

  private String extractIP(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private ResponseEntity<?> createSuccessfulRedirectToResponse(ShortURL l) {
    HttpHeaders h = new HttpHeaders();
    h.setLocation(URI.create(l.getTarget()));
    return new ResponseEntity<>(h, HttpStatus.valueOf(l.getMode()));
  }

  /**
   * 
   * @param inputURL URL to being shortened
   * @return True, if and only if, inputURL is reachable.
   */
  private boolean reachableURL(String inputURL){
    HttpURLConnection httpURLConn;
    try{
      httpURLConn = (HttpURLConnection) new URL(inputURL).openConnection();
      // HEAD request is like GET request but just expecting headers, not resources
      httpURLConn.setRequestMethod("HEAD");
      // System.out.println("Status request: " + httpURLConn.getResponseCode())
      return httpURLConn.getResponseCode() == HttpURLConnection.HTTP_OK;
    }
    catch(Exception e){
      System.out.println("Error: " + e.getMessage());
      return false;
    }
  }
}
