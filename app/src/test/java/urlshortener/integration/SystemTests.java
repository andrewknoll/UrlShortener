package urlshortener.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import net.glxn.qrgen.javase.QRCode;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@RunWith(SpringRunner.class)
@DirtiesContext
public class SystemTests {

  @Autowired
  private TestRestTemplate restTemplate;

  @LocalServerPort
  private int port;

  @Value("classpath:testUrls.csv")
  Resource testCSVFile;

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
    assertTrue(entity.getHeaders().getContentType().isCompatibleWith(new MediaType("text", "html")));
    assertThat(entity.getBody(), containsString("<title>URL"));
  }

  @Test
  public void testCss() {
    ResponseEntity<String> entity = restTemplate.getForEntity("/webjars/bootstrap/3.3.5/css/bootstrap.min.css",
        String.class);
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
    parts.add("url", "http://example.com/");
    parts.add("generateQR", "true");
    ResponseEntity<String> entity = restTemplate.postForEntity("/link", parts, String.class);

    int tries = 0;
    while (tries < MAX_TRIES && JsonPath.parse(entity.getBody()).read("$.qrUri") == null) {
      Thread.sleep(1000);
      tries++;
    }

    assertThat(entity.getStatusCode(), is(HttpStatus.CREATED));
    assertThat(entity.getHeaders().getLocation(), is(new URI("http://localhost:" + this.port + "/f684a3c4")));
    assertThat(entity.getHeaders().getContentType(), is(new MediaType("application", "json")));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.hash"), is("f684a3c4"));
    assertThat(rc.read("$.uri"), is("http://localhost:" + this.port + "/f684a3c4"));
    assertThat(rc.read("$.target"), is("http://example.com/"));
    assertThat(rc.read("$.sponsor"), is(nullValue()));

    assertThat(JsonPath.parse(entity.getBody()).read("$.qrUri"), is("http://localhost:" + this.port + "/qr/f684a3c4"));
  }

  @Ignore
  @Test
  public void testQR() throws Exception {

    ResponseEntity<String> entityLink = postLink("http://example.com/");

    String hash = JsonPath.parse(entityLink.getBody()).read("$.hash");
    ResponseEntity<byte[]> entityQR = restTemplate.getForEntity("/qr/" + hash, byte[].class);

    assertThat(entityQR.getStatusCode(), is(HttpStatus.ACCEPTED));
    assertThat(entityQR.getHeaders().getLocation(), is(new URI("http://localhost:" + this.port + "/qr/f684a3c4")));
    assertThat(entityQR.getHeaders().getContentType(), is(new MediaType("application", "octet-stream"))); // is byte
                                                                                                          // stream?

    assertThat(entityQR.getHeaders().get("hash").size(), is(1)); // has only one hash?
    assertThat(entityQR.getHeaders().get("hash").get(0), is("f684a3c4")); // hash is correct?

    byte[] rc = entityQR.getBody();

    QRCode qr = QRCode.from(JsonPath.parse(entityLink.getBody()).read("$.uri").toString());
    assertArrayEquals(toByteArray(qr), rc); // code is correct?

  }

  @Test
  public void testRedirection() throws Exception {
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

  @Test
  public void testGoogleSafeBrowsingCheck() throws Exception {

    // Safe URL
    ResponseEntity<String> entity = postLink("https://google.com/");

    ReadContext rc = JsonPath.parse(entity.getBody());
    String safeUrlHash = rc.read("$.hash");

    ResponseEntity<String> safeRE = restTemplate.getForEntity("/" + safeUrlHash, String.class);

    // Tries to query the endpoint 10 times or until it is verified by the async
    // process
    int tries = 0;
    while (tries < MAX_TRIES && safeRE.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
      Thread.sleep(1000);
      safeRE = restTemplate.getForEntity("/" + safeUrlHash, String.class);
      tries++;
    }
    // Checks redirection occured after safety was verified
    assertThat(safeRE.getStatusCode(), is(HttpStatus.TEMPORARY_REDIRECT));

    // Unsafe URL
    ResponseEntity<String> unsafeEntity = postLink(
        "https://mrnxajqdmrmdwkrpenecpvmkozeusfipwpgw-dot-offgl8876899977678.uk.r.appspot.com/xxx");
    rc = JsonPath.parse(unsafeEntity.getBody());
    String unsafeUrlHash = rc.read("$.hash");

    ResponseEntity<String> unsafeRE = restTemplate.getForEntity("/" + unsafeUrlHash, String.class);
    rc = JsonPath.parse(unsafeRE.getBody());
    String error = rc.read("$.error");

    // Tries to query the endpoint until google safe browsing confirms it is not
    // safe asyncronously
    tries = 0;
    while (tries < MAX_TRIES && error.equals("Aun no verificada")) {
      Thread.sleep(1000);
      unsafeRE = restTemplate.getForEntity("/" + unsafeUrlHash, String.class);
      rc = JsonPath.parse(unsafeRE.getBody());
      error = rc.read("$.error");
      tries++;
    }
    // Checks the url is marked as unsafe and therefore isn't redirected
    assertThat(rc.read("$.error"), is("URL marcada por Google Safe Browsing como SOCIAL_ENGINEERING"));
    assertThat(unsafeRE.getStatusCode(), is(HttpStatus.FORBIDDEN));

  }

  @Test
  public void checkCSVFunctionality() throws IOException {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    // Reading CSV file
    FileSystemResource csvFileResource = new FileSystemResource(testCSVFile.getFile());
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("urls", csvFileResource);
    HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
    ResponseEntity<String> response = restTemplate.postForEntity("/multipleLinks", entity, String.class);

    String responseBody = response.getBody();
    responseBody = responseBody.replace("data:text/csv;charset=utf-8,", "");
    String[] lines = responseBody.split("\n");
    String[] firstLine = lines[0].split(",");
    String[] lastLine = lines[4].split(",");

    // Should receive a csv file with 5 lines
    assertEquals(lines.length, 5);
    // First line containing the original URL of the original CSV file
    assertEquals(firstLine[0], "https://developer.mozilla.org/en-US/docs/Web/API/FileReader");
    // Since it is a valid URL, there shouldn't be any problems
    assertEquals(firstLine.length, 2);
    // Last URL isn't valid and therefore should have some information notifying it
    assertEquals(lastLine.length, 3);
    assertEquals(lastLine[2], "debe ser una URI http o https");
    // Statuscode should be 201
    assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
    // Location header should cointain the short url of the first shortened url
    assertEquals(response.getHeaders().getLocation().toString(), firstLine[1]);

  }

  private ResponseEntity<String> postLink(String url) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("url", url);
    return restTemplate.postForEntity("/link", parts, String.class);
  }

}