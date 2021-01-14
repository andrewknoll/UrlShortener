package urlshortener.eip;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.DataFormat;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.URLDecoder;

@Component
public class Router extends RouteBuilder {

  public static final String QR_URI = "direct:qr";

  
  public String HOST1;
  public String HOST2;

  @Autowired
  public Router(@Value("${host1}") String host1, @Value("${host2}") String host2) {
      this.HOST1 = host1;
      this.HOST2 = host2;
  }

  @Override
  public void configure() throws Exception {

    onException(Exception.class)
      .handled(true)
      .to("direct:errors");
    
    from(QR_URI)
      .routeId("qr")
      .streamCaching()
      .setHeader("Accept", simple("image/png"))
      .setHeader(Exchange.HTTP_METHOD, constant("GET"))
      .loadBalance()
      .failover(2, false, true) //roundrobin enabled, sends to next machine if failure occurs
        .toD(this.HOST1 + "/qr?origin=${body}&hash=${header.hash}").id("tohost1")
        .toD(this.HOST2 + "/qr?origin=${body}&hash=${header.hash}").id("tohost2")
      .end();

    from("direct:errors")
        .routeId("errors")
        .process(new Decoder())
        .setBody(simple("ERROR while trying to generate QR code for address ${body}/${header.hash}. Perhaps QR service is down?"))
        .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500));
  }
  
  private class Decoder implements Processor{
    public void process(Exchange exchange) throws Exception {
      String message = exchange.getIn().getBody(String.class);
      message = URLDecoder.decode(message, "UTF-8");
      exchange.getIn().setBody(message);
    }
  }
}
