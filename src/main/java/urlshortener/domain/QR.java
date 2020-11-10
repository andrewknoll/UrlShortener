package urlshortener.domain;

import java.io.File;
import java.net.URI;

public class QR {

  private final String hash;

  private final String fileName;
  private final URI uri;
  private final File code;

  public QR(String hash, String fileName, URI uri, File code) {
    this.hash = hash;
    this.fileName = fileName;
    this.uri = uri;
    this.code = code;
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

  public File getQR() {
    return code;
  }
};