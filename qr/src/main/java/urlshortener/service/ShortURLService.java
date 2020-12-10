package urlshortener.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.stereotype.Service;
import urlshortener.domain.ShortURL;
import urlshortener.repository.ShortURLRepository;
import urlshortener.web.UrlShortenerController;
import urlshortener.web.QRController;

@Service
public class ShortURLService {

  private final ShortURLRepository shortURLRepository;

  public ShortURLService(ShortURLRepository shortURLRepository) {
    this.shortURLRepository = shortURLRepository;
  }

  public ShortURL findByKey(String id) {
    return shortURLRepository.findByKey(id);
  }

  public ShortURL save(String url, String sponsor, String ip, boolean qrWasGenerated) {
    ShortURL su = ShortURLBuilder.newInstance()
        .target(url)
        .uri((String hash) -> linkTo(methodOn(UrlShortenerController.class).redirectTo(hash, null))
            .toUri())
        .sponsor(sponsor)
        .createdNow()
        .randomOwner()
        .temporaryRedirect()
        .treatAsSafe()
        .ip(ip)
        .unknownCountry()
        .qrGenerated((String hash) -> linkTo(methodOn(QRController.class).retrieveQRCodebyHash(hash, null, null))
            .toUri(), qrWasGenerated)
        .description("Aun no verificada")
        .build();
    return shortURLRepository.save(su);
  }

  public void updateShortUrl(ShortURL su, boolean safe, String description) {
    su.setSafe(safe);
    su.setDescription(description);
    shortURLRepository.update(su);
  }
}
