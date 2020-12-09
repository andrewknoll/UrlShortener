package urlshortener.web;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static urlshortener.fixtures.ShortURLFixture.someUrl;
import static urlshortener.fixtures.ShortURLFixture.url1;
import static urlshortener.fixtures.QRFixture.qr1;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import net.glxn.qrgen.javase.QRCode;
import urlshortener.domain.QR;
import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.QRService;
import urlshortener.service.ShortURLService;

public class UrlShortenerTests {

  private MockMvc mockMvc;

  @Mock
  private ClickService clickService;

  @Mock
  private ShortURLService shortUrlService;

  @Mock
  private QRService qrService;

  @InjectMocks
  private UrlShortenerController urlShortener;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mockMvc = MockMvcBuilders.standaloneSetup(urlShortener).build();
  }

  @Test
  public void thatRedirectToReturnsTemporaryRedirectIfKeyExists()
      throws Exception {
    when(shortUrlService.findByKey("someKey")).thenReturn(someUrl());

    mockMvc.perform(get("/{id}", "someKey")).andDo(print())
        .andExpect(status().isTemporaryRedirect())
        .andExpect(redirectedUrl("http://example.com/"));
  }

  @Test
  public void thatRedirecToReturnsNotFoundIdIfKeyDoesNotExist()
      throws Exception {
    when(shortUrlService.findByKey("someKey")).thenReturn(null);

    mockMvc.perform(get("/{id}", "someKey")).andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  public void thatShortenerCreatesARedirectIfTheURLisOK() throws Exception {
    configureSave(null, null);

    mockMvc.perform(post("/link").param("url", "http://example.com/"))
        .andDo(print())
        .andExpect(redirectedUrl("http://localhost/f684a3c4"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.hash", is("f684a3c4")))
        .andExpect(jsonPath("$.uri", is("http://localhost/f684a3c4")))
        .andExpect(jsonPath("$.target", is("http://example.com/")))
        .andExpect(jsonPath("$.sponsor", is(nullValue())));
  }

  @Test
  public void thatShortenerCreatesARedirectWithSponsor() throws Exception {
    configureSave("http://sponsor.com/", null);

    mockMvc.perform(
        post("/link").param("url", "http://example.com/").param(
            "sponsor", "http://sponsor.com/")).andDo(print())
        .andExpect(redirectedUrl("http://localhost/f684a3c4"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.hash", is("f684a3c4")))
        .andExpect(jsonPath("$.uri", is("http://localhost/f684a3c4")))
        .andExpect(jsonPath("$.target", is("http://example.com/")))
        .andExpect(jsonPath("$.sponsor", is("http://sponsor.com/")));
  }

  @Test
  public void thatShortenerFailsIfTheURLisWrong() throws Exception {
    configureSave(null, null);

    mockMvc.perform(post("/link").param("url", "someKey")).andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void thatShortenerFailsIfTheRepositoryReturnsNull() throws Exception {
    when(shortUrlService.save(any(String.class), any(String.class), any(String.class), anyBoolean())).thenReturn(null);

    mockMvc.perform(post("/link").param("url", "someKey")).andDo(print()).andExpect(status().isBadRequest());
  }
  
  @Test
  public void thatShortenerCreatesAQRIfTheHashisStored() throws Exception {

    ByteArrayOutputStream oos = new ByteArrayOutputStream();
    QRCode.from("http://localhost/f684a3c4").writeTo(oos);

    configureSave(null, new URI("http://localhost/qr/f684a3c4"));
    configureSaveQR();
    when(shortUrlService.findByKey("f684a3c4")).thenReturn(urlWithParameters(null, new URI("http://localhost/qr/f684a3c4")));

    mockMvc.perform(get("/qr/{hash}.{format}", "f684a3c4", "png")).andDo(print()).andExpect(status().isAccepted())
        .andExpect(header().string("hash", is("f684a3c4")))
        .andExpect(header().string("Location", is("http://localhost/f684a3c4")))
        .andExpect(content().bytes(oos.toByteArray()));
  }
  
  @Test
  public void thatRetrieveQRCodeByHashReturnsAcceptedIfKeyExists()
      throws Exception {

    ByteArrayOutputStream oos = new ByteArrayOutputStream();
    QRCode.from("http://localhost/f684a3c4").writeTo(oos);

    when(shortUrlService.findByKey("f684a3c4")).thenReturn(url1());
    when(qrService.findByHash("f684a3c4")).thenReturn(qr1());

    mockMvc.perform(get("/qr/{hash}", "f684a3c4")).andDo(print()).andExpect(status().isAccepted())
        .andExpect(content().bytes(oos.toByteArray()));
  }

  @Test
  public void thatRetrieveQRByHashReturnsNotFoundIdIfKeyDoesNotExist()
      throws Exception {
    when(qrService.findByHash("someKey")).thenReturn(null);

    mockMvc.perform(get("/qr/{hash}", "someKey")).andDo(print()).andExpect(status().isNotFound());
  }

  private void configureSave(String sponsor, URI qrUri) {
    when(shortUrlService.save(any(), any(), any(), anyBoolean()))
        .then((Answer<ShortURL>) invocation -> urlWithParameters(sponsor, qrUri));
  }
  
  private void configureSaveQR() throws InterruptedException, ExecutionException{
    when(qrService.save(any()))
        .then((Answer<CompletableFuture<QR>>) invocation -> 
        CompletableFuture.completedFuture(new QR("f684a3c4", URI.create("http://localhost/f684a3c4"), qr1().getQR())));
  }
  
  private ShortURL urlWithParameters(String sponsor, URI qrUri) {
    return new ShortURL(
            "f684a3c4",
            "http://example.com/",
            URI.create("http://localhost/f684a3c4"),
            sponsor,
            null,
            null,
            0,
            false,
            null,
            null,
            qrUri,
            null);
  }

}
