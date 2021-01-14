package urlshortener.fixtures;

import java.io.ByteArrayOutputStream;

import net.glxn.qrgen.javase.QRCode;
import urlshortener.domain.QR;

public class QRFixture {

  public static byte[] toByteArray(QRCode qr) {
    ByteArrayOutputStream oos = new ByteArrayOutputStream();
    qr.writeTo(oos);
    return oos.toByteArray();
  }

  public static QR qr1() {
    return new QR("1", null, toByteArray(QRCode.from("something/f684a3c4")));
  }
}
