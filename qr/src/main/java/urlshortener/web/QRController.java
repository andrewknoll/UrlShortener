package urlshortener.web;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import urlshortener.domain.QR;
import urlshortener.domain.ShortURL;
import urlshortener.service.QRService;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RestController
public class QRController {

  private final QRService qrService;

  public QRController(QRService qrService) {
    this.qrService = qrService;
  }

    @RequestMapping(value = { "/qr/{su}"}, method = RequestMethod.GET)
    public ResponseEntity<?> retrieveQRCodebyHash(@PathVariable ShortURL su, HttpServletRequest request) {
      try {
        QR q = qrService.createQR(su).get(); //Generate QR
        HttpHeaders h = new HttpHeaders();
        h.add("hash", su.getHash());
        h.setLocation(q.getUri());
        h.setCacheControl(cacheConfig());
        return new ResponseEntity<byte[]>(q.getQR(), h, HttpStatus.ACCEPTED);
      } catch (InterruptedException e) {
        return error("Async worker has been interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (ExecutionException e) {
        return error("Async worker has errored" + e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    
    private CacheControl cacheConfig() {
        return CacheControl.maxAge(10, TimeUnit.DAYS).cachePublic();
    }

    private ResponseEntity<?> error(String message, HttpStatus status) {
        HashMap<String, String> map = new HashMap<>();
        map.put("error", message);
        return new ResponseEntity<>(map, status);
      }
}