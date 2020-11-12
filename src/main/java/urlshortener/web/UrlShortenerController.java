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

@RestController
public class UrlShortenerController {
  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final QRService qrService;

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
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
                                            @RequestParam(value = "sponsor", required = false)
                                                String sponsor,
                                                HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http",
        "https"});
    if (urlValidator.isValid(url) && this.reachableURL(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());
      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
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
