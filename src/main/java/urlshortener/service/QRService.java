package urlshortener.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import org.springframework.stereotype.Service;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import urlshortener.domain.QR;
import urlshortener.domain.ShortURL;
import urlshortener.repository.QRRepository;
import urlshortener.web.UrlShortenerController;

@Service
public class QRService {

  private final QRRepository QRRepository;

  public QRService(QRRepository QRRepository) {
    this.QRRepository = QRRepository;
  }

  public QR findByHash(String id) {
    return QRRepository.findByHash(id);
  }

  public QR save(ShortURL su) {
    ByteArrayOutputStream oos = new ByteArrayOutputStream();
    URI uri = linkTo(methodOn(UrlShortenerController.class).redirectTo(su.getHash(), null)).toUri();
    QRCode.from(uri.toString()).to(ImageType.PNG).writeTo(oos);
    QR qr = QRBuilder.newInstance()
        .hash(su.getHash())
        .uri((String h) -> linkTo(methodOn(UrlShortenerController.class).retrieveQRCodebyHash(h, null, null)).toUri())
        .code(oos.toByteArray()).build();
    return QRRepository.save(qr);
  }
}
