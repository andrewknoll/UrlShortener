package urlshortener.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import urlshortener.domain.QR;
import urlshortener.domain.ShortURL;
import urlshortener.repository.QRRepository;
import urlshortener.repository.impl.QRCache;
import urlshortener.web.UrlShortenerController;
import org.springframework.core.env.Environment;

import java.net.URISyntaxException;
import java.net.UnknownHostException;

import static urlshortener.eip.Router.QR_URI;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class QRService {

  @Autowired
  private Environment environment;

  private final QRRepository QRRepository;

  public QRService(QRRepository QRRepository) {
    this.QRRepository = QRRepository;
  }

  public QR findByHash(String id) {
    QRCache cache = QRCache.getInstance();
    QR result = cache.find(id);
    if (result != null) {
      return result;
    }
    return cache.put(id, QRRepository.findByHash(id));
  }

  @Async("asyncWorker")
  public CompletableFuture<QR> save(ShortURL su) throws InterruptedException {
    ByteArrayOutputStream oos = new ByteArrayOutputStream();
    URI uri = linkTo(methodOn(UrlShortenerController.class).redirectTo(su.getHash(), null)).toUri();
    QRCode.from(uri.toString()).to(ImageType.PNG).writeTo(oos);
    QR qr = QRBuilder.newInstance().hash(su.getHash()).uri((String h) -> {
      try {
        return new URI(getServerIP() + QR_URI + h);
      } catch (Exception e) {
        return null;
      }
    }).code(oos.toByteArray()).build();

    return CompletableFuture.completedFuture(QRRepository.save(qr));
  }

  private String getServerIP() throws UnknownHostException {
    return InetAddress.getLocalHost().getHostAddress() + environment.getProperty("server.port");
  }
}
