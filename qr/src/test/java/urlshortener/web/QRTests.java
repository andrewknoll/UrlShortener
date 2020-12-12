package urlshortener.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static urlshortener.fixtures.QRFixture.qr1;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import net.glxn.qrgen.javase.QRCode;
import urlshortener.domain.QR;
import urlshortener.service.QRService;

@Ignore
public class QRTests {

  private MockMvc mockMvc;

  @Mock
  private QRService qrService;

  @InjectMocks
  private QRController qrcontroller;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mockMvc = MockMvcBuilders.standaloneSetup(qrcontroller).build();
  }
  @Test
  public void thatControllerCreatesAQRIfTheHashisStored() throws Exception {

    ByteArrayOutputStream oos = new ByteArrayOutputStream();
    QRCode.from("http://localhost/f684a3c4").writeTo(oos);

    configureSaveQR();

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

    mockMvc.perform(get("/qr/{hash}", "f684a3c4")).andDo(print()).andExpect(status().isAccepted())
        .andExpect(content().bytes(oos.toByteArray()));
  }

  private void configureSaveQR() throws InterruptedException, ExecutionException{
    when(qrService.createQR(any()))
        .then((Answer<CompletableFuture<QR>>) invocation -> 
        CompletableFuture.completedFuture(new QR("f684a3c4", URI.create("http://localhost/f684a3c4"), qr1().getQR())));
  }

}
