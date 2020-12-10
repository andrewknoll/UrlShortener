package urlshortener.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.stereotype.Service;
import urlshortener.domain.ShortURL;
import urlshortener.repository.ShortURLRepository;
import urlshortener.web.UrlShortenerController;
import org.springframework.core.env.Environment;

import static urlshortener.eip.Router.QR_URI;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.URISyntaxException;

@Service
public class ShortURLService {

  @Autowired
  private Environment environment;

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
        .qrGenerated((String h) -> {
          try{
            return new URI(getServerIP() + QR_URI + h);
          }
          catch(URISyntaxException e){
            return null;
          }
        }, qrWasGenerated)
        .description("Aun no verificada")
        .build();
    return shortURLRepository.save(su);
  }

  public void updateShortUrl(ShortURL su, boolean safe, String description) {
    su.setSafe(safe);
    su.setDescription(description);
    shortURLRepository.update(su);
  }

  private String getServerIP(){
    return environment.getProperty("server.address") + environment.getProperty("server.port");
  }
}
