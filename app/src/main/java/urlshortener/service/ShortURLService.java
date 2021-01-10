package urlshortener.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.stereotype.Service;
import urlshortener.domain.ShortURL;
import urlshortener.repository.ShortURLRepository;
import urlshortener.web.UrlShortenerController;
import java.net.URI;

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
    ShortURL su = ShortURLBuilder.newInstance().target(url)
        .uri((String hash) -> linkTo(methodOn(UrlShortenerController.class).redirectTo(hash, null)).toUri())
        .sponsor(sponsor).createdNow().randomOwner().temporaryRedirect().treatAsUnsafe().ip(ip).unknownCountry()
        .qrGenerated((String hash) -> {
          try {
            return linkTo(methodOn(UrlShortenerController.class).retrieveQRCodebyHash(hash, null, null)).toUri();
          } catch (Exception e) {
            return null;
          }
        }, qrWasGenerated).description("Aun no verificada").build();

    return shortURLRepository.save(su);
  }

  public void updateShortUrl(ShortURL su, URI uri, boolean safe, String description) {
    su.setUri(uri);
    su.setSafe(safe);
    su.setDescription(description);
    shortURLRepository.update(su);
  }
}
