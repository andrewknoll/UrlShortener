package urlshortener.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.client.ExpectedCount.between;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.ReadContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.language.bean.Bean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import net.glxn.qrgen.javase.QRCode;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration()
@DirtiesContext
public class SystemTests {

  @Autowired
  private TestRestTemplate restTemplate;

  @LocalServerPort
  private int port;


  RestTemplate qrRestTemplate = new RestTemplate();
  MockRestServiceServer qrServer = MockRestServiceServer.bindTo(qrRestTemplate).build();

  @Value("${host1}")
  private String HOST1;

  @Value("${host2}")
  private String HOST2;

  private final int MAX_TRIES = 10;

  public static byte[] toByteArray(QRCode qr) {
    ByteArrayOutputStream oos = new ByteArrayOutputStream();
    qr.writeTo(oos);
    return oos.toByteArray();
  }

  @Test
  public void testHome() {
    ResponseEntity<String> entity = restTemplate.getForEntity("/", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.OK));
    assertNotNull(entity.getHeaders().getContentType());
    assertTrue(
        entity.getHeaders().getContentType().isCompatibleWith(new MediaType("text", "html")));
    assertThat(entity.getBody(), containsString("<title>URL"));
  }

  @Test
  public void testCss() {
    ResponseEntity<String> entity =
        restTemplate.getForEntity("/webjars/bootstrap/3.3.5/css/bootstrap.min.css", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.OK));
    assertThat(entity.getHeaders().getContentType(), is(MediaType.valueOf("text/css")));
    assertThat(entity.getBody(), containsString("body"));
  }

  @Test
  public void testCreateLink() throws Exception {
    ResponseEntity<String> entity = postLink("http://example.com/");

    assertThat(entity.getStatusCode(), is(HttpStatus.CREATED));
    assertThat(entity.getHeaders().getLocation(), is(new URI("http://localhost:" + this.port + "/f684a3c4")));
    assertThat(entity.getHeaders().getContentType(), is(new MediaType("application", "json")));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.hash"), is("f684a3c4"));
    assertThat(rc.read("$.uri"), is("http://localhost:" + this.port + "/f684a3c4"));
    assertThat(rc.read("$.target"), is("http://example.com/"));
    assertThat(rc.read("$.sponsor"), is(nullValue()));
  }
  
  @Test
  public void testCreateLinkWithQR() throws Exception {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("url", "http://google.com/");
    parts.add("generateQR", "true");
    ResponseEntity<String> entity = restTemplate.postForEntity("/link", parts, String.class);

    int tries = 0;
    while (tries < MAX_TRIES && checkQRUri(entity)) {
      Thread.sleep(1000);
      tries++;
    }

    assertThat(entity.getStatusCode(), is(HttpStatus.CREATED));
    assertThat(entity.getHeaders().getLocation(),
        is(new URI("http://localhost:" + this.port + "/5e399431")));
    assertThat(entity.getHeaders().getContentType(), is(new MediaType("application", "json")));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.hash"), is("5e399431"));
    assertThat(rc.read("$.uri"), is("http://localhost:" + this.port + "/5e399431"));
    assertThat(rc.read("$.target"), is("http://google.com/"));
    assertThat(rc.read("$.sponsor"), is(nullValue()));

    assertThat(JsonPath.parse(entity.getBody()).read("$.qrUri"), is("http://localhost:" + this.port + "/qr/5e399431"));
  }

  @Test
  public void testRedirectionToSponsor() throws Exception {
    ResponseEntity<String> entityURL = postLink("http://example.com/");
    int tries = 0;
    while (tries < MAX_TRIES && JsonPath.parse(entityURL.getBody()).read("$.safe").toString().equals("false")) {
      Thread.sleep(1000);
      tries++;
    }
    
    ResponseEntity<String> entity = restTemplate.getForEntity("/f684a3c4", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.TEMPORARY_REDIRECT));
    assertThat(entity.getHeaders().getLocation(), is(new URI("http://example.com/")));
  }

  private ResponseEntity<String> postLink(String url) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("url", url);
    return restTemplate.postForEntity("/link", parts, String.class);
  }

  private boolean checkQRUri(ResponseEntity<String> entity){
    try{
      return JsonPath.parse(entity.getBody()).read("$.qrUri") == null;
    }
    catch(PathNotFoundException e){
      return true;
    }
  }


}