package urlshortener.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.stereotype.Service;

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

  public QR findByName(String id) {
    return QRRepository.findByName(id);
  }

  public QR findByHash(String id) {
    return QRRepository.findByHash(id);
  }

  public QR save(ShortURL su, String fileName) {
    QR qr = QRBuilder.newInstance()
        .hash(su.getHash())
        .fileName(fileName)
        .uri((String h, String f) -> linkTo(methodOn(UrlShortenerController.class).getQRCode(h, f, null)).toUri())
        .code(QRCode.from(su.getUri().getPath()).file()).build();
    return QRRepository.save(qr);
  }
}
