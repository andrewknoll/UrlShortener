package urlshortener.web;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.*;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndProxy;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpForward;
import org.mockserver.verify.VerificationTimes;

import net.glxn.qrgen.javase.QRCode;

import java.io.IOException;
import org.mockserver.model.HttpResponse;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static org.mockserver.integration.ClientAndProxy.startClientAndProxy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpClassCallback.callback;
import static org.mockserver.model.HttpForward.forward;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.StringBody.exact;
import org.mockserver.model.Parameter;

public class TestMockQRServer {

  private static ClientAndServer mockServer;

    @BeforeClass
    public static void startServer() {
        mockServer = startClientAndServer(1080);
    }


  private void mockQRCreation(String hash) {
    QRCode qr = QRCode.from("http://localhost:8080/" + hash);
    HttpResponse response = response()
                            .withStatusCode(201)
                            .withHeaders(
                              new Header("Content-Type", "image/png"),
                              new Header("Cache-Control", "public, max-age=864000"))
                            .withBody(qr.toString())
                            .withDelay(TimeUnit.SECONDS,1);
    Parameter p[] = {new Parameter ("origin", "localhost"),
        new Parameter("hash", hash) };
    new MockServerClient("127.0.0.1", 8179)
      .when(
        request()
          .withMethod("GET")
          .withPath("/qr")
          .withQueryStringParameters(p),
          exactly(1))
            .respond(
              response
            );

    new MockServerClient("127.0.0.1", 8180)
    .when(
      request()
        .withMethod("GET")
        .withPath("/qr")
        .withQueryStringParameters(p),
        exactly(1))
          .respond(
            response
          );
    }
    // ...

    @AfterClass
    public static void stopServer() {
        mockServer.stop();
    }
}