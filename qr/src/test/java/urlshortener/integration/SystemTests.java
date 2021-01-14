package urlshortener.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.jayway.jsonpath.JsonPath;

import java.io.ByteArrayOutputStream;
import java.net.URI;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import net.glxn.qrgen.javase.QRCode;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext
public class SystemTests {

  @Autowired
  private TestRestTemplate restTemplate;

  @LocalServerPort
  private int port;

  public static byte[] toByteArray(QRCode qr) {
    ByteArrayOutputStream oos = new ByteArrayOutputStream();
    qr.writeTo(oos);
    return oos.toByteArray();
  }

  @Test
  public void testQR() throws Exception {

    ResponseEntity<byte[]> entityQR = restTemplate.getForEntity("/qr?origin=http://localhost:" + this.port + "&hash=f684a3c4", byte[].class);

    assertThat(entityQR.getStatusCode(), is(HttpStatus.ACCEPTED));
    assertThat(entityQR.getHeaders().getLocation(),
        is(new URI("http://localhost:" + this.port + "/qr/f684a3c4")));
    assertThat(entityQR.getHeaders().getContentType(), is(new MediaType("image", "png"))); //is byte stream?

    assertThat(entityQR.getHeaders().get("hash").size(), is(1));  //has only one hash?
    assertThat(entityQR.getHeaders().get("hash").get(0), is("f684a3c4")); //hash is correct?

    byte[] rc = entityQR.getBody();

    QRCode qr = QRCode.from("http://localhost:" + this.port + "/f684a3c4");
    assertArrayEquals(toByteArray(qr), rc); //code is correct?
    
  }
}