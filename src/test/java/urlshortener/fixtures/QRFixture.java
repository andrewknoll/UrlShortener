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
    return new QR("1", "file1.png", null, toByteArray(QRCode.from("http://localhost/f684a3c4")));
  }

  public static QR qr1modified() {
    return new QR("1", "file1.png", null, toByteArray(QRCode.from("http://localhost/1")));
  }

  public static QR qr2() {
    return new QR("2", "file2.png", null, toByteArray(QRCode.from("http://www.unizar.es")));
  }

  public static QR qr3() {
    return new QR("3", "file3.png", null, toByteArray(QRCode.from("http://www.google.com")));
  }

  public static QR badqr() {
    return new QR(null, null, null, null);
  }
}
