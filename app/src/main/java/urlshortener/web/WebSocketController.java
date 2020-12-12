package urlshortener.web;


import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.QRService;
import urlshortener.service.SafeCheckService;
import urlshortener.service.ShortURLService;

import javax.json.Json;


@Component
public class WebSocketController extends RouteBuilder {

    private final ShortURLService shortUrlService;

    private final ClickService clickService;

    private final QRService qrService;

    private final SafeCheckService safeCheckService;

    public WebSocketController(ShortURLService shortUrlService, ClickService clickService, QRService qrService, SafeCheckService safeCheckService) {
        this.shortUrlService = shortUrlService;
        this.clickService = clickService;
        this.qrService = qrService;
        this.safeCheckService = safeCheckService;
    }


    @Override
    public void configure() {

        from("websocket://localhost:50000/link").process(exchange -> {
            String url = exchange.getIn().getBody(String.class);
            UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"}, UrlValidator.ALLOW_2_SLASHES);
            if (urlValidator.isValid(url)) {
                ShortURL su = shortUrlService.save(url, "", "",false);
                safeBrowsingCheck(su, url);
                exchange.getIn().setBody("http://localhost:8080" + su.getUri().toString());
            } else {
                exchange.getIn().setBody("Invalid url");
            }
        }).to("websocket://localhost:50000/link");

    }
    public void safeBrowsingCheck(ShortURL su, String url) {
        UrlShortenerController.googleSafeBrowsing(su, url, safeCheckService, shortUrlService);
    }
}

