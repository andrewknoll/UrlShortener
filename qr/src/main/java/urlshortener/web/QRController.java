package urlshortener.web;

import javax.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import urlshortener.domain.QR;
import urlshortener.service.QRService;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import java.net.URLDecoder;

@RestController
@OpenAPIDefinition(info = @Info(title = "QR Service API", description = "REST API to generate QR codes", version = "1.0"))
public class QRController {

  private final QRService qrService;

  public QRController(QRService qrService) {
    this.qrService = qrService;
  }

  @RequestMapping(value = { "/qr" }, method = RequestMethod.GET, produces = "image/png")
  @Operation(method = "GET", description = "Returns a QR code for URL corresponding to the indicated parameters (origin/hash)")
  @Parameter(name = "hash", required = true, example = "f684a3c4")
  @Parameter(name = "origin", description = "URL where the UrlShortenerController is hosted", required = false)
  @ApiResponses(value = { @ApiResponse(responseCode = "202", description = "QR code was asynchronously created"),
          @ApiResponse(responseCode = "500", description = "An unexpected exception was thrown") })
  public ResponseEntity<?> retrieveQRCodebyHash(@RequestParam("origin") String origin, @RequestParam("hash") String hash,
      HttpServletRequest request) {
      try {
        origin = URLDecoder.decode(origin, "UTF-8");
        System.out.println(origin);
        QR q = qrService.createQR(origin, hash).get(); // Generate QR
        HttpHeaders h = new HttpHeaders();
        h.add("hash", hash);
        h.setLocation(q.getUri());
        h.setCacheControl(cacheConfig());
        return new ResponseEntity<byte[]>(q.getQR(), h, HttpStatus.ACCEPTED);
      } catch (InterruptedException e) {
        return error("Async worker has been interrupted", HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (ExecutionException e) {
        return error("Async worker has errored" + e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (URISyntaxException e) {
        return error("Malformed URI on QR generation" + e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (UnsupportedEncodingException e) {
        return error("Unknown Encoding" + e.getCause(), HttpStatus.INTERNAL_SERVER_ERROR);
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