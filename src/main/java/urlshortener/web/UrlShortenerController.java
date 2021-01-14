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
import org.springframework.http.ContentDisposition;
import org.apache.http.client.ClientProtocolException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import urlshortener.domain.QR;
import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.QRService;
import urlshortener.service.SafeCheckService;
import urlshortener.service.ShortURLService;
import urlshortener.repository.impl.SponsorCache;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static urlshortener.eip.Router.QR_URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URLEncoder;

@RestController
@OpenAPIDefinition(info = @Info(title = "URL Shortener API", description = "REST API to shorten URLs", version = "1.0"))
public class UrlShortenerController {

  private static final Logger log = LoggerFactory.getLogger(UrlShortenerController.class);

  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final QRService qrService;

  private final ProducerTemplate producerTemplate;

  private final SafeCheckService safeCheckService;

  private final String defaultFormat = "png";

  // Path to sponsor.html file
  private Resource sponsorResource;

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService, QRService qrService,
      SafeCheckService safeCheckService, ProducerTemplate producerTemplate,
      @Value("classpath:static/sponsor.html") Resource sponsorResource) throws IOException {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
    this.qrService = qrService;
    this.safeCheckService = safeCheckService;
    this.producerTemplate = producerTemplate;
    this.sponsorResource = sponsorResource;
  }

  /**
   * 
   * @param url        input URL to being shorted
   * @param sponsor    sponsor assigned in case we had some
   * @param generateQR input to know if we have to generate a QR
   * @param request    HTTPServletRequest
   * @return Status code 201 if object created and validated without problems
   *         Status code 206 if problems creating QR Status code 400 and json
   *         error message if introduced URI does not begin with http or https
   * @throws ClientProtocolException
   * @throws IOException
   * @throws URISyntaxException
   */

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  @Operation(method = "POST", description = "Given an url, it returns the short url that redirects to the original. A QR code can also be generated for the short url")
  @Parameters(value = {
      @Parameter(name = "url", description = "URL to shorten", required = true, example = "https://example.com/"),
      @Parameter(name = "sponsor", description = "Name of the sponsor", required = false, example = "BBVA"),
      @Parameter(name = "generateQR", description = "Choose to generate a QR for the shortened URL") })
  @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Short URL was successfully created."),
      @ApiResponse(responseCode = "206", description = "Short url was generated but qr code wasn't"),
      @ApiResponse(responseCode = "400", description = "The provided URL doesn't have a valid format (doesnt't start with http or https).") })
  public ResponseEntity<?> shortener(@RequestParam("url") String url,
      @RequestParam(value = "sponsor", required = false) String sponsor,
      @RequestParam(value = "generateQR", defaultValue = "false") boolean generateQR, HttpServletRequest request)
      throws ClientProtocolException, IOException, URISyntaxException {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" }, UrlValidator.ALLOW_2_SLASHES);

    if (urlValidator.isValid(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr(), generateQR);
      if (su == null) {
        su = shortUrlService.findByTarget(url).get(0);
      }
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
      String json = Json.createObjectBuilder().add("error", "Debe ser una URI http o https").build().toString();
      return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
    }

  }

  /**
   *
   * @param String             id hash from shorted URL
   * @param HttpServletRequest request HTTPServletRequest
   * @return String Status code 307 and sponsor page if hash exists Status code
   *         400 if final URI unreachable Status code 403 if Google Safe Browsing
   *         does not validate Status code 404 if hash does not exist
   */
  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  @Operation(method = "GET", description = "Redirects from the shortened URL to target URL")
  @Parameter(name = "id", description = "Hash associated to the target URL", required = true, example = "f684a3c4")
  @ApiResponses(value = { @ApiResponse(responseCode = "307", description = "Redirecting to the target URL."),
      @ApiResponse(responseCode = "403", description = "The target URL has not been verified yet or has been marked by Google Safe Browsing as unsafe.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ error: \"URL marked by Google Safe Browsing as SOCIAL_ENGINEERING\"}"))),
      @ApiResponse(responseCode = "404", description = "ID not found un the system.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ error: \"Provided hash has not been found or is not valid\"}"))),
      @ApiResponse(responseCode = "400", description = "The target URL is not reachable.", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{ error: \"URI not reachable\"}"))) })
  public ResponseEntity<?> redirectTo(@PathVariable String id, HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      String finalURL = l.getTarget();
      // If URL is safe and reachable, redirect
      if (l.getSafe() && this.reachableURL(finalURL)) {
        clickService.saveClick(id, extractIP(request));
        return redirectThroughSponsor();
      } else if (!l.getSafe()) {
        // If not safe return Forbidden 403
        String json = Json.createObjectBuilder().add("error", l.getDescription()).build().toString();
        return new ResponseEntity<>(json, HttpStatus.FORBIDDEN);
      } else {
        // If not reachable return status code 400
        String json = Json.createObjectBuilder().add("error", "URI not reachable").build().toString();
        return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
      }
    } else {
      return error("Provided hash has not been found or is not valid", HttpStatus.NOT_FOUND);
    }
  }

  /**
   *
   * @param hash ShortURL id
   * @return Server Sent Events Emitter that is going to send finalURI after 5
   *         seconds to redirect
   */
  @RequestMapping(value = "/redirect/{hash:(?!link|index).*}", method = RequestMethod.GET)
  @Operation(method = "GET", description = "Redirects to target URL through a http event.")
  @Parameter(name = "hash", description = "Hash associated to the target URL", required = true, example = "f684a3c4")
  public SseEmitter getFinalURI(@PathVariable String hash) {
    long sleepingTimeMS = 5000;
    SseEmitter emitter = new SseEmitter();

    String URI = "";
    ShortURL l = shortUrlService.findByKey(hash);
    if (l != null) {
      URI = l.getTarget();
    }
    String finalURI = URI;

    try {
      Thread.sleep(sleepingTimeMS);
      emitter.send(finalURI);
    } catch (Exception e) {
      emitter.completeWithError(e);
    }
    return emitter;
  }

  @RequestMapping(value = { "/qr/{hash}", "/qr/{hash}.{format}" }, method = RequestMethod.GET, produces = "image/png")
  @Operation(method = "GET", description = "Retrieves a QR code for a given previously saved hash, either from database or from the QR service endpoints")
  @Parameter(name = "hash", required = true, example = "f684a3c4")
  @Parameter(name = "format", description = "Image format. Only accepts PNG in this version", required = false)
  @ApiResponses(value = { @ApiResponse(responseCode = "202", description = "QR code received correctly from QR endpoints, or retrieved from database / cache"),
      @ApiResponse(responseCode = "500", description = "An unexpected exception was thrown, or QR code could not be retrieved / received") })
  public ResponseEntity<?> retrieveQRCodebyHash(@PathVariable String hash,
      @PathVariable(required = false) String format, HttpServletRequest request) throws URISyntaxException {
    if (defaultFormat.equals(format) || format == null) {
      QR q = qrService.findByHash(hash); // Try to find if QR was already generated
      ShortURL su = shortUrlService.findByKey(hash); // Try to find ShortUrl
      try {
        if (su != null) {
          shortUrlService.updateShortUrl(su, new URI(extractLocalAddress(request) + "/" + su.getHash()), su.getSafe(),
              su.getDescription());
          if (q == null) { // if QR was never generated

            Exchange exchange = producerTemplate.send(QR_URI, new Processor() {
              public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody(extractLocalAddress(request));
                exchange.getIn().setHeader("hash", su.getHash());
              }
            });
            Message out = exchange.getOut();
            Integer responseCode = out.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
            if (responseCode.intValue() == HttpStatus.ACCEPTED.value()) {
              return new ResponseEntity<>(out.getBody(byte[].class), HttpStatus.ACCEPTED);
            } else {
              return new ResponseEntity<>(out.getBody(), HttpStatus.resolve(responseCode.intValue()));
            }
          } else {
            HttpHeaders h = new HttpHeaders();
            h.add("hash", hash);
            h.setLocation(q.getUri());
            h.setCacheControl(cacheConfig(10));
            return new ResponseEntity<byte[]>(q.getQR(), h, HttpStatus.ACCEPTED);
          }
        }
      } catch (Exception e) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(e, h, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    return error("Provided hash has not been found or is not valid", HttpStatus.NOT_FOUND);
  }

  /**
   *
   * @param urls CSV file with the urls to shorten
   * @return The result CSV in String format with the Status code 201 if the
   *         processing was successfull and all urls were shortened
   * @return Statuscode 500 if there was an IOexception reading the csv file
   * @throws IOException
   */
  @RequestMapping(value = "/multipleLinks", method = RequestMethod.POST, produces = "text/csv")
  @Operation(method = "POST", description = "Given a CSV file with URLs, it returns another CSV containing the original URLs, as well as their corresponding short URLs and information about the problems with the target URL if there are any.")
  @Parameter(name = "urls", description = "CSV File containing the target URLs", required = true, example = "Urls.csv")

  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Output CSV file created successfully", content = @Content(mediaType = "text/csv")),
      @ApiResponse(responseCode = "500", description = "IO exception processing the file") })
  public ResponseEntity<String> multipleShortener(@RequestParam("urls") MultipartFile urls,
      HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });

    try {
      // Local variables
      BufferedReader br;
      List<String> urlsList = new ArrayList<>();
      List<String> problems = new ArrayList<>();
      List<String> resultList = new ArrayList<>();
      String resultString;
      ShortURL firstSu = new ShortURL();
      InputStream is = urls.getInputStream();

      // Save received CSV urls
      br = new BufferedReader(new InputStreamReader(is));
      while ((resultString = br.readLine()) != null) {
        urlsList.add(resultString);
      }

      // Check if all urls are valid and safe
      // Otherwise save the problem
      for (int i = 0; i < urlsList.size(); i++) {
        if (!urlValidator.isValid(urlsList.get(i))) {
          problems.add(i, "debe ser una URI http o https");
        } else {
          problems.add(i, "");
        }
      }

      // Save shortened urls only if there wasn't a problem with them
      for (int i = 0; i < urlsList.size(); i++) {
        if (problems.get(i).isEmpty()) {
          ShortURL su = shortUrlService.save(urlsList.get(i), "", request.getRemoteAddr(), false);
          resultList.add(su.getUri().toString());

          // Save the first URL
          if (resultList.size() == 1) {
            firstSu = su;
          }
          // Async process to check if the URLs are safe against Google Safe Browsing
          safeBrowsingCheck(su, su.getTarget());
        } else {
          resultList.add("");
        }
      }

      // Create response string
      resultString = "data:text/csv;charset=utf-8,";
      for (int i = 0; i < resultList.size(); i++) {
        resultString = resultString + (urlsList.get(i) + ',' + resultList.get(i) + "," + problems.get(i) + "\n");
      }

      // Set response headers
      HttpHeaders responseHeaders = new HttpHeaders();
      ContentDisposition cd = ContentDisposition.builder("attachment").filename("Short-urls.csv").build();
      responseHeaders.setLocation(firstSu.getUri());
      responseHeaders.setContentDisposition(cd);
      return new ResponseEntity<>(resultString, responseHeaders, HttpStatus.CREATED);

    } catch (IOException e) {
      log.error(e.toString());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  // Method to check if the provided URL is safe with the google safe browsing API
  public void safeBrowsingCheck(ShortURL su, String url) {
    googleSafeBrowsing(su, url, safeCheckService, shortUrlService);
  }

  /**
   * 
   * @param su               short url that will be updated once checked
   * @param url              url to verify
   * @param safeCheckService service to contact with google safe browsing to
   *                         perform the check
   * @param shortUrlService  service to update the ShortURL
   */
  public static void googleSafeBrowsing(ShortURL su, String url, SafeCheckService safeCheckService,
      ShortURLService shortUrlService) {
    try {
      // Async process call for verification
      safeCheckService.safeBrowsingChecker(url).thenAcceptAsync((result) -> {
        // Update database
        if (result.get(0).equals("SAFE")) {
          shortUrlService.updateShortUrl(su, su.getUri(), true, "");
        } else {
          shortUrlService.updateShortUrl(su, su.getUri(), false, result.get(1));
        }
      }).exceptionally(e -> {
        shortUrlService.updateShortUrl(su, su.getUri(), false, "No se ha podido verificar con google Safe Browsing");
        return null;
      });
    } catch (Exception e) {
      log.error(e.toString());
      shortUrlService.updateShortUrl(su, su.getUri(), false, "No se ha podido verificar con google Safe Browsing");
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
      SponsorCache sc = SponsorCache.getInstance();
      String data = sc.find("sponsor");
      // Cached
      if (data == null) {
        data = new String(Files.readAllBytes(resource.toPath()));
        data = sc.put("sponsor", data);
      }
      // Shows sponsor.html page without changing location
      HttpHeaders h = new HttpHeaders();
      h.setCacheControl(cacheConfig(1));
      return new ResponseEntity<>(data, h, HttpStatus.TEMPORARY_REDIRECT);
    } catch (IOException e) {
      e.printStackTrace();
      log.error(e.toString());
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);

    }
  }

  private String extractIP(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private String extractLocalAddress(HttpServletRequest request) throws UnsupportedEncodingException {
    InetAddressValidator validator = InetAddressValidator.getInstance();
    String address = request.getLocalAddr();
    if (validator.isValidInet6Address(address)) {
      address = "[" + address + "]";
    }
    return URLEncoder.encode("http://" + address + ":" + request.getLocalPort(), "UTF-8");

  }

  /**
   * 
   * @param inputURL URL to being shortened
   * @return True, if and only if, inputURL is reachable (200 <= status code <
   *         400)
   */
  private boolean reachableURL(String inputURL) {
    HttpURLConnection httpURLConn;
    try {
      httpURLConn = (HttpURLConnection) new URL(inputURL).openConnection();
      // HEAD request is like GET request but just expecting headers, not resources
      httpURLConn.setRequestMethod("HEAD");
      // System.out.println("Status request: " + httpURLConn.getResponseCode())
      int HTTP_SUCCESS = 200; // Success HTTP code
      int HTTP_ERROR = 400; // Error HTTP code (starting)
      int responseCode = httpURLConn.getResponseCode();
      return (HTTP_SUCCESS <= responseCode) && (responseCode < HTTP_ERROR);
    } catch (Exception e) {
      log.error("Error: " + e.getMessage());
      return false;
    }
  }

  private ResponseEntity<?> error(String message, HttpStatus status) {
    HashMap<String, String> map = new HashMap<>();
    map.put("error", message);
    HttpHeaders h = new HttpHeaders();
    h.setContentType(MediaType.APPLICATION_JSON);
    return new ResponseEntity<>(map, h, status);
  }

  private CacheControl cacheConfig(int maxAge) {
    return CacheControl.maxAge(maxAge, TimeUnit.DAYS).cachePublic();
  }
}
