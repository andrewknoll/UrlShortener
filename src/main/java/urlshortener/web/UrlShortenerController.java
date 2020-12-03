package urlshortener.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
      // If not safe return bad request
      if (l.getSafe()) {
        clickService.saveClick(id, extractIP(request));
        return createSuccessfulRedirectToResponse(l);
      } else {
        String json = Json.createObjectBuilder().add("error", l.getDescription()).build().toString();
        return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
      }
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<String> shortener(@RequestParam("url") String url,
      @RequestParam(value = "sponsor", required = false) String sponsor, HttpServletRequest request)
      throws ClientProtocolException, IOException {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });

    if (urlValidator.isValid(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());

      // Async process to check if URL is safe
      Thread safeCheckThread = new Thread(() -> {
        try {
          List<String> safeBrowsingResult = safeBrowsingChecker(url);
          if (safeBrowsingResult.get(0).equals("SAFE")) {
            shortUrlService.updateShortUrl(su, true, "");
          } else {
            shortUrlService.updateShortUrl(su, false, safeBrowsingResult.get(1));
          }
        } catch (Exception e) {
          shortUrlService.updateShortUrl(su, false, "No se ha podido verificar con google Safe Browsing");
          System.out.println("Exception in thread");
        }

      });

      safeCheckThread.start();

      return new ResponseEntity<>(su.getUri().toString(), h, HttpStatus.CREATED);

    } else {
      String json = Json.createObjectBuilder().add("error", "debe ser una URI http o https").build().toString();
      return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
    }

  }

  @RequestMapping(value = "/multiplelLinks", method = RequestMethod.POST, produces = "text/csv")
  public ResponseEntity<String> multipleShortener(@RequestParam("urls") MultipartFile urls,
      HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https" });

    try {
      System.out.println("Received type " + urls.getContentType());
      BufferedReader br;

      List<String> urlsList = new ArrayList<>();
      List<String> problems = new ArrayList<>();

      String line;
      InputStream is = urls.getInputStream();
      br = new BufferedReader(new InputStreamReader(is));
      while ((line = br.readLine()) != null) {
        urlsList.add(line);
      }
      // Check if all urls are valid and safe
      for (int i = 0; i < urlsList.size(); i++) {
        if (!urlValidator.isValid(urlsList.get(i))) {
          problems.add(i, "debe ser una URI http o https");
        } else {
          problems.add(i, "");
        }
      }

      List<String> resultList = new ArrayList<>();
      ShortURL firstSu = new ShortURL();

      for (int i = 0; i < urlsList.size(); i++) {
        if (problems.get(i).isEmpty()) {
          ShortURL su = shortUrlService.save(urlsList.get(i), "", request.getRemoteAddr());
          // Save the first URL
          if (resultList.size() == 1) {
            firstSu = su;
          }
          resultList.add(su.getUri().toString());

          // Async process to check if URLs are safe
          Thread safeCheckThread = new Thread(() -> {
            try {
              List<String> safeBrowsingResult = safeBrowsingChecker(su.getTarget());
              if (safeBrowsingResult.get(0).equals("SAFE")) {
                shortUrlService.updateShortUrl(su, true, "");
              } else {
                shortUrlService.updateShortUrl(su, false, safeBrowsingResult.get(1));
              }
            } catch (Exception e) {
              shortUrlService.updateShortUrl(su, false, "No se ha podido verificar con google Safe Browsing");
              System.out.println("Exception in thread");
            }

          });

          safeCheckThread.start();

        } else {
          resultList.add("");
        }

      }

      // FileWriter fileWriter = new FileWriter("csvResponse.csv");
      String resultString = "data:text/csv;charset=utf-8,";

      for (int i = 0; i < resultList.size(); i++) {
        resultString = resultString + (urlsList.get(i) + ',' + resultList.get(i) + "," + problems.get(i) + "\n");
      }

      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setLocation(firstSu.getUri());

      MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
      headers.add("Location", resultList.get(0));

      return new ResponseEntity<>(resultString, headers, HttpStatus.CREATED);

    } catch (Exception e) {
      System.out.println("Exception  " + e.toString());
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public List<String> safeBrowsingChecker(String url) throws ClientProtocolException, IOException {
    List<String> resultList = new ArrayList<>();

    List<String> urlsList = new ArrayList<>();
    urlsList.add(url);

    String json = buildPayload(urlsList);

    String safeBrowsingUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=AIzaSyD2-m3JAvdEYiAzPLhF-uVN2ZUIW6MkXU4";

    HttpPost post = new HttpPost(safeBrowsingUrl);
    post.addHeader("content-type", "application/json");
    String result = "";
    int code = 0;

    // send a JSON data
    try {
      post.setEntity(new StringEntity(json));

      try (CloseableHttpClient httpClient = HttpClients.createDefault();
          CloseableHttpResponse response = httpClient.execute(post)) {
        result = EntityUtils.toString(response.getEntity());
        code = response.getStatusLine().getStatusCode();
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      resultList.add(0, "REQUEST_ERROR");
      resultList.add(1, "Error enviando peticion a Google Safe Browsing");
      return resultList;
    }
    JsonReader jsonReader = Json.createReader(new StringReader(result));
    JsonObject jsonObject = jsonReader.readObject();
    jsonReader.close();

    if (code != 200) {
      resultList.add(0, "REQUEST_ERROR");
      resultList.add(1, "Google Safe Browsing ha devuelto statuscode " + code);
      return resultList;
    }

    // Not a threat
    if (jsonObject.isEmpty()) {
      resultList.add("SAFE");
      return resultList;
    } else {

      // A threat
      JsonArray threats = jsonObject.getJsonArray("matches");
      for (int i = 0; i < threats.size(); i++) {
        String currentThreatType = threats.getJsonObject(i).getString("threatType");
        resultList.add(0, "UNSAFE");
        resultList.add(1, "URL marcada por Google Safe Browsing como  " + currentThreatType);
      }

      return resultList;
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
