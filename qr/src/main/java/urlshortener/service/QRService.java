package urlshortener.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import urlshortener.domain.QR;
import urlshortener.web.QRController;

@Service
public class QRService {

  @Async("asyncWorker")
  public CompletableFuture<QR> createQR(String origin, String hash) throws InterruptedException, URISyntaxException {
    ByteArrayOutputStream oos = new ByteArrayOutputStream();
    URI uri = new URI(origin + "/" + hash);
    QRCode.from(uri.toString()).to(ImageType.PNG).writeTo(oos);
    QR qr = QRBuilder.newInstance()
        .hash(hash)
        .uri((String h) -> linkTo(methodOn(QRController.class).retrieveQRCodebyHash(origin, hash, null)).toUri())
        .code(oos.toByteArray()).build();

    return CompletableFuture.completedFuture(qr);
  }
}
