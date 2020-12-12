package urlshortener.domain;
import java.net.URI;

public class QR {

  private String hash;

  private URI uri;
  private byte[] code;

  public QR(String hash, URI uri, byte[] code) {
    this.hash = hash;
    this.uri = uri;
    this.code = code;
  }

  public QR() {
  }

  public String getHash() {
    return hash;
  }

  public URI getUri() {
    return uri;
  }

  public byte[] getQR() {
    return code;
  }
};