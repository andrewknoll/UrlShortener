package urlshortener.web;

import com.jayway.jsonpath.JsonPath;
import net.glxn.qrgen.core.image.ImageType;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.reifier.RouteReifier;
import org.apache.camel.spring.boot.SpringBootCamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.*;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.Test;

import static org.bouncycastle.util.Strings.toByteArray;
import static org.hamcrest.Matchers.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndProxy;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpForward;
import org.mockserver.verify.VerificationTimes;

import net.glxn.qrgen.javase.QRCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.mockserver.model.HttpResponse;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockserver.integration.ClientAndProxy.startClientAndProxy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpClassCallback.callback;
import static org.mockserver.model.HttpForward.forward;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mockserver.model.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import urlshortener.eip.Router;
import org.apache.camel.test.spring.junit5.CamelSpringTest;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@UseAdviceWith
public class TestCamelRoutingForQR{

  @Autowired
  protected TestRestTemplate restTemplate;

  @Autowired
  protected CamelContext camelContext;

  @Value("${host1}")
  private String host1;
  @Value("${host2}")
  private String host2;

  @EndpointInject("mock:host1/qr")
  protected MockEndpoint mock1;

  @EndpointInject("mock:host2/qr")
  protected MockEndpoint mock2;

  private byte[] mockedQr;

  public void mockEndPoints() throws Exception {
    mockedQr = mockQRCreation();
    ModelCamelContext mcc = camelContext.adapt(ModelCamelContext.class);
    RouteReifier.adviceWith(mcc.getRouteDefinition("qr"), mcc, new AdviceWithRouteBuilder() {
      @Override
      public void configure() throws Exception {
          weaveByType(ToDynamicDefinition.class)
                  .replace()
                  .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                      exchange.getOut().setBody(mockedQr);
                      exchange.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 202);
                    }
                  });
      }
    });
  }
  @Test
  public void testRouteWithCorrectMessage() throws Exception {

    mockEndPoints();
    camelContext.start();

    ResponseEntity<String> link = postLink("http://www.google.es");

    String hash = JsonPath.parse(link.getBody()).read("$.hash");
    ResponseEntity<byte[]> qr = restTemplate.getForEntity("/qr/" + hash, byte[].class);

    assertThat(qr.getStatusCode(), is(HttpStatus.ACCEPTED));
    assertArrayEquals(qr.getBody(), mockedQr);

    }

    private ResponseEntity<String> postLink(String url) {
      MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
      parts.add("url", url);
      return restTemplate.postForEntity("/link", parts, String.class);
    }

    public byte[] mockQRCreation() {
      ByteArrayOutputStream oos = new ByteArrayOutputStream();
      QRCode.from("mocked_QR").to(ImageType.PNG).writeTo(oos);
      return oos.toByteArray();
    }

}






