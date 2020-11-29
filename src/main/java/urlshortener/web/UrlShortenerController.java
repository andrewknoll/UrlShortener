package urlshortener.web;

import org.springframework.http.MediaType;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import urlshortener.domain.QR;
import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.QRService;
import urlshortener.service.ShortURLService;

import java.util.HashMap;

@RestController
public class UrlShortenerController {
  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final QRService qrService;

  private final String defaultFormat = "png";

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService, QRService qrService) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
    this.qrService = qrService;
  }

  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id,
      HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      clickService.saveClick(id, extractIP(request));
      return createSuccessfulRedirectToResponse(l);
    } else {
      return error("Provided hash has not been found or is not valid", HttpStatus.NOT_FOUND);
    }
  }
  
  @RequestMapping(value = { "/qr/{hash}", "qr/{hash}.{format}" }, method = RequestMethod.GET)
  public ResponseEntity<?> retrieveQRCodebyHash(@PathVariable String hash,
      @PathVariable(required = false) String format,
      HttpServletRequest request) {
    if (defaultFormat.equals(format) || format == null) {
      QR q = qrService.findByHash(hash); //Try to find if QR was already generated
      ShortURL su = shortUrlService.findByKey(hash); //Try to find ShortUrl
      if (su != null) {
        clickService.saveClick(hash, extractIP(request));
        if (q == null) { //if QR was never generated
          q = qrService.save(su); //Generate QR
        }
        HttpHeaders h = new HttpHeaders();
        h.add("hash", hash);
        h.setLocation(q.getUri());
        return new ResponseEntity<byte[]>(q.getQR(), h, HttpStatus.ACCEPTED);
      }
    }
    return error("Provided hash has not been found or is not valid", HttpStatus.NOT_FOUND);
    
  }

  /*
  @RequestMapping(value = "/qr", method = RequestMethod.POST, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<byte[]> generateQRCode(@RequestParam("hash") String hash,
                                     @RequestParam(value = "generateQR", defaultValue = "false") boolean generateQR,
                                     HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(hash);
    if (l != null) {
      if (generateQR) {
        QR qr = qrService.save(l);
      }
      HttpHeaders h = new HttpHeaders();
      h.setLocation(qr.getUri());
      h.add("hash", qr.getHash());
      return new ResponseEntity<>(qr.getQR(), h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
*/
  @RequestMapping(value = "/link", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> shortener(@RequestParam("url") String url,
                                            @RequestParam(value = "sponsor", required = false)
                                                String sponsor,
                                            @RequestParam(value = "generateQR", defaultValue = "false")
                                                boolean generateQR,
                                                HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http",
        "https"});
    if (urlValidator.isValid(url) && this.reachableURL(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr(), generateQR);
      if (generateQR) {
        qrService.save(su);
      }
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());
      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      return error("Provided URL is not valid or is unreachable", HttpStatus.BAD_REQUEST);
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
}
