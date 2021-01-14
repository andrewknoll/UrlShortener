package urlshortener.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.Map;

import net.minidev.json.JSONObject;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.QRService;
import urlshortener.service.SafeCheckService;
import urlshortener.service.ShortURLService;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketComponent {

    @Autowired
    Environment environment;

    @Autowired
    private ShortURLService shortUrlService;

    @Autowired
    private ClickService clickService;

    @Autowired
    private SafeCheckService safeCheckService;

    @MessageMapping("/user")
    @SendTo("/reply/shorturl")
    public JSONUrl getShortUrl(String url) throws UnknownHostException {
        UrlValidator urlValidator = new UrlValidator(new String[]{"http", "https"},
                UrlValidator.ALLOW_2_SLASHES);
        if (urlValidator.isValid(url)) {
            ShortURL su = shortUrlService.save(url, null, null, false);
            safeBrowsingCheck(su, url);
            return new JSONUrl("http://" + localName(), su.getHash());
        } else {
            return null;
        }
    }

    private String localName() throws UnknownHostException {
        String port = environment.getProperty("local.server.port");
        return InetAddress.getLocalHost().getHostAddress() + ":" + port;
    }

    public void safeBrowsingCheck(ShortURL su, String url) {
        UrlShortenerController.googleSafeBrowsing(su, url, safeCheckService, shortUrlService);
    }


    private class JSONUrl{
        private String hash;
        private String hostname;

        public JSONUrl(String hostname, String hash) {
            this.hash = hash;
            this.hostname = hostname;
        }

        public String getHash() {
            return hash;
        }

        public void setHash(String hash) {
            this.hash = hash;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }
    }

}