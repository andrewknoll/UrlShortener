package urlshortener.eip;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

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

//TODO:Devolver algo por defecto (opcional)
  @Override
  public void configure() {
    from(QR_URI)
      .loadBalance()
      .roundRobin()
        .toD(this.HOST1 + "/qr/${body}")
        .toD(this.HOST2 + "/qr/${body}")
      .end();
  }
}
