package urlshortener.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SafeCheckService {

    static public Logger logger = LoggerFactory.getLogger(SafeCheckService.class);
    @Value("${secret_key}")
    private String SECRET_GOOGLE_SAFE_BROWSING_KEY;

    @Async("asyncWorker")
    public CompletableFuture<List<String>> safeBrowsingChecker(String url) throws IOException {
        List<String> resultList = new ArrayList<>();
        List<String> urlsList = new ArrayList<>();
        urlsList.add(url);
    
        String json = buildPayload(urlsList);
    
        String safeBrowsingUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + SECRET_GOOGLE_SAFE_BROWSING_KEY;
    
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
          }catch (Exception e ){
              System.out.println("Exception in closeableclient");
          }
        } catch (UnsupportedEncodingException e) {
          e.printStackTrace();
          resultList.add(0, "REQUEST_ERROR");
          resultList.add(1, "Error enviando peticion a Google Safe Browsing");
            System.out.println("Request error");
            return CompletableFuture.completedFuture(resultList);
        }
        JsonReader jsonReader = Json.createReader(new StringReader(result));
        JsonObject jsonObject = jsonReader.readObject();
        jsonReader.close();

        if (code != 200) {
          resultList.add(0, "REQUEST_ERROR");
          resultList.add(1, "Google Safe Browsing ha devuelto statuscode " + code);
            return CompletableFuture.completedFuture(resultList);
        }
    
        // Not a threat
        if (jsonObject.isEmpty()) {
          resultList.add("SAFE");
        } else {
    
          // A threat
          JsonArray threats = jsonObject.getJsonArray("matches");
          for (int i = 0; i < threats.size(); i++) {
            String currentThreatType = threats.getJsonObject(i).getString("threatType");
            resultList.add(0, "UNSAFE");
            resultList.add(1, "URL marcada por Google Safe Browsing como  " + currentThreatType);
          }

        }
        return CompletableFuture.completedFuture(resultList);
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
}
