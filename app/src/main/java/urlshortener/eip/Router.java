package urlshortener.eip;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class Router extends RouteBuilder {

  public static final String QR_URI = "direct:qr";

  
  public static String HOST1;
  public static String HOST2;

  @Autowired
  public Router(@Value("${host1}") String HOST1, @Value("${host2}") String HOST2) {
      this.HOST1 = HOST1;
      this.HOST2 = HOST2;
  }


  @Override
  public void configure() {
    from(QR_URI)
      .loadBalance()
      .roundRobin()
        .toD("HOST1/qr/${body}")
        .toD("HOST2/qr/${body}")
      .end();
  }
}
