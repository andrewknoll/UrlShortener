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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RestController
public class QRController {

  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final QRService qrService;

  private final String defaultFormat = "png";

  public QRController(ShortURLService shortUrlService, ClickService clickService, QRService qrService) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
    this.qrService = qrService;
  }

    @RequestMapping(value = { "/qr/{hash}", "qr/{hash}.{format}" }, method = RequestMethod.GET)
    public ResponseEntity<?> retrieveQRCodebyHash(@PathVariable String hash, @PathVariable(required = false) String format, HttpServletRequest request) {
    try {
        if (defaultFormat.equals(format) || format == null) {
        QR q = qrService.findByHash(hash); //Try to find if QR was already generated
        ShortURL su = shortUrlService.findByKey(hash); //Try to find ShortUrl
        if (su != null) {
            clickService.saveClick(hash, extractIP(request));
            if (q == null) { //if QR was never generated
            q = qrService.save(su).get(); //Generate QR
            }
            HttpHeaders h = new HttpHeaders();
            h.add("hash", hash);
            h.setLocation(q.getUri());
            h.setCacheControl(cacheConfig());
            return new ResponseEntity<byte[]>(q.getQR(), h, HttpStatus.ACCEPTED);
        }
        }
    }
    catch (InterruptedException e) {
        return error("Async worker has been interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    catch(ExecutionException e){
        return error("Async worker has errored" + e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return error("Provided hash has not been found or is not valid", HttpStatus.NOT_FOUND);
    }

    private CacheControl cacheConfig() {
        return CacheControl.maxAge(10, TimeUnit.DAYS).cachePublic();
    }

    private ResponseEntity<?> error(String message, HttpStatus status) {
        HashMap<String, String> map = new HashMap<>();
        map.put("error", message);
        return new ResponseEntity<>(map, status);
      }
      
    private String extractIP(HttpServletRequest request) {
        return request.getRemoteAddr();
      }
}