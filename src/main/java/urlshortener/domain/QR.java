package urlshortener.domain;
import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QR {

  private String hash;

  private String fileName;
  private URI uri;
  private byte[] code;

  public QR(String hash, String fileName, URI uri, byte[] code) {
    this.hash = hash;
    this.fileName = fileName;
    this.uri = uri;
    this.code = code;
  }

  public QR() {
  }

  public String getHash() {
    return hash;
  }

  public String getFileName() {
    return fileName;
  }

  public URI getUri() {
    return uri;
  }

  public byte[] getQR() {
    return code;
  }
};