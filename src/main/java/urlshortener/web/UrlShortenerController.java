package urlshortener.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.ShortURLService;

@RestController
public class UrlShortenerController {
  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
  }

  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id, HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      clickService.saveClick(id, extractIP(request));
      return createSuccessfulRedirectToResponse(l);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
      @RequestParam(value = "sponsor", required = false) String sponsor, HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });
    System.out.print("Received url " + url);
    if (urlValidator.isValid(url)) {
      System.out.println(" - success");

      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());
      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      System.out.println(" - error");
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  @RequestMapping(value = "/multiplelLinks", method = RequestMethod.POST)
  public ResponseEntity<List<String>> multipleShortener(@RequestParam("urls") MultipartFile urls,
      HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });

    try {
      System.out.println("Received type " + urls.getContentType());
      BufferedReader br;
      List<String> urlsList = new ArrayList<>();
      String line;
      InputStream is = urls.getInputStream();
      br = new BufferedReader(new InputStreamReader(is));
      while ((line = br.readLine()) != null) {
        System.out.println(line);
        urlsList.add(line);
      }
      // Check if all urls are valid
      for (String url : urlsList) {
        if (!urlValidator.isValid(url)) {
          return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
      }
      List<String> resultList = new ArrayList<>();

      for (String url : urlsList) {
        ShortURL su = shortUrlService.save(url, "", request.getRemoteAddr());
        System.out.println("Result url " + su.getUri().toString());
        resultList.add(su.getUri().toString());
      }
      return new ResponseEntity<>(resultList, HttpStatus.OK);

    } catch (Exception e) {
      System.out.println("Exception  " + e.toString());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @RequestMapping(value = "/safeBrowsing", method = RequestMethod.POST)
  public ResponseEntity<String> safeBrowsingChecker(@RequestParam("url") String url,
      @RequestParam(value = "multiple", required = false, defaultValue = "false") boolean multiple,
      HttpServletRequest request) throws ClientProtocolException, IOException {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });

    String[] urlsStrings = url.split(",");

    if (multiple) {
      // Checks if all urls are valid
      for (String tmpUrl : urlsStrings) {
        if (!urlValidator.isValid(tmpUrl)) {
          System.out.println("Invalid url " + tmpUrl);
          return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
      }
    } else {
      if (!urlValidator.isValid(url)) {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
      }
    }

    List<String> urlsList = new LinkedList<String>();

    for (String tmpUrl : urlsStrings) {
      urlsList.add(tmpUrl);
    }
    String json = buildPayload(urlsList);

    String safeBrowsingUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyD2-m3JAvdEYiAzPLhF-uVN2ZUIW6MkXU4";

    HttpPost post = new HttpPost(safeBrowsingUrl);
    post.addHeader("content-type", "application/json");
    String result = "";
    // send a JSON data
    post.setEntity(new StringEntity(json));
    int code = 0;
    try (CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = httpClient.execute(post)) {
      result = EntityUtils.toString(response.getEntity());
      code = response.getStatusLine().getStatusCode();
    }
    JsonReader jsonReader = Json.createReader(new StringReader(result));
    JsonObject jsonObject = jsonReader.readObject();
    jsonReader.close();

    if (code != 200) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /*
     * Map<String, ?> config = new HashMap<String, Object>(); JsonBuilderFactory
     * factory = Json.createBuilderFactory(config); JsonObject badJson =
     * Json.createObjectBuilder() .add("matches", factory.createArrayBuilder()
     * .add(factory.createObjectBuilder().add("threatType", "MALWARE").add("threat",
     * factory.createObjectBuilder().add("url", url)))
     * .add(factory.createObjectBuilder().add("threatType",
     * "SOCIAL_ENGINEERING	").add("threat",
     * factory.createObjectBuilder().add("url", "http://secondurl.com")))) .build();
     */

    // Not a threat
    if (jsonObject.isEmpty()) {
      return new ResponseEntity<>(HttpStatus.OK);
    } else {
      String threatType = "";
      String threatUrl = "";
      // A threat
      JsonArray threats = jsonObject.getJsonArray("matches");
      for (int i = 0; i < threats.size(); i++) {
        String currentThreatType = threats.getJsonObject(i).getString("threatType");
        switch (currentThreatType) {
          case "POTENTIALLY_HARMFUL_APPLICATION":
            currentThreatType = "Potentially harmful application";
            break;
          case "MALWARE":
            currentThreatType = "Malware";
            break;
          case "SOCIAL_ENGINEERING":
            currentThreatType = "Phising";
            break;
          case "UNWANTED_SOFTWARE":
            currentThreatType = "Unwanted software";
            break;
          default:
            currentThreatType = "Unkown";
            break;
        }

        // Last threat
        if (i == threats.size() - 1) {
          threatType = threatType + currentThreatType;
          threatUrl = threatUrl + threats.getJsonObject(i).getJsonObject("threat").getString("url");
        } else {
          threatType = threatType + currentThreatType + ",";
          threatUrl = threatUrl + threats.getJsonObject(i).getJsonObject("threat").getString("url") + ",";

        }
      }
      System.out.println("Urls " + threatUrl);
      System.out.println("Marked as " + threatType);
      threatUrl = threatUrl + ";" + threatType;

      return new ResponseEntity<>(threatUrl, HttpStatus.FORBIDDEN);
    }

  }

  private static String buildPayload(List<String> urls) {

    Map<String, ?> config = new HashMap<String, Object>();
    JsonBuilderFactory factory = Json.createBuilderFactory(config);
    // Create Payload
    String json = "";
    if (urls.size() == 1) {
      // Payload with 1 url
      json = Json.createObjectBuilder()
          .add("client",
              factory.createObjectBuilder().add("clientId", "urlshortener-295117").add("clientVersion", "1.5.2"))
          .add("threatInfo",
              factory.createObjectBuilder()
                  .add("threatTypes",
                      factory.createArrayBuilder().add("THREAT_TYPE_UNSPECIFIED").add("MALWARE")
                          .add("SOCIAL_ENGINEERING").add("UNWANTED_SOFTWARE").add("POTENTIALLY_HARMFUL_APPLICATION"))
                  .add("platformTypes", factory.createArrayBuilder().add("ANY_PLATFORM"))
                  .add("threatEntryTypes", factory.createArrayBuilder().add("URL")).add("threatEntries",
                      factory.createArrayBuilder().add(factory.createObjectBuilder().add("url", urls.get(0)))))
          .build().toString();

    } else {
      // Payload with with multiple urls
      JsonArrayBuilder threatEntriesarray = factory.createArrayBuilder();
      for (int i = 0; i < urls.size(); i++) {
        threatEntriesarray.add(factory.createObjectBuilder().add("url", urls.get(i)));
      }
      json = Json.createObjectBuilder()
          .add("client",
              factory.createObjectBuilder().add("clientId", "urlshortener-295117").add("clientVersion", "1.5.2"))
          .add("threatInfo",
              factory.createObjectBuilder()
                  .add("threatTypes",
                      factory.createArrayBuilder().add("THREAT_TYPE_UNSPECIFIED").add("MALWARE")
                          .add("SOCIAL_ENGINEERING").add("UNWANTED_SOFTWARE").add("POTENTIALLY_HARMFUL_APPLICATION"))
                  .add("platformTypes", factory.createArrayBuilder().add("ANY_PLATFORM"))
                  .add("threatEntryTypes", factory.createArrayBuilder().add("URL"))
                  .add("threatEntries", threatEntriesarray))
          .build().toString();
    }
    return json;
  }

  private String extractIP(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private ResponseEntity<?> createSuccessfulRedirectToResponse(ShortURL l) {
    HttpHeaders h = new HttpHeaders();
    h.setLocation(URI.create(l.getTarget()));
    return new ResponseEntity<>(h, HttpStatus.valueOf(l.getMode()));
  }
}
