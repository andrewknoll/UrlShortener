package urlshortener.service;

import java.net.URI;
import java.util.function.Function;

import urlshortener.domain.QR;

public class QRBuilder {

  private String hash;
  private URI uri;
  private byte[] code;

  static QRBuilder newInstance() {
    return new QRBuilder();
  }

  QR build() {
    return new QR(hash, uri, code);
  }

  QRBuilder hash(String hash) {
    this.hash = hash;
    return this;
  }

  QRBuilder code(byte[] code) {
    this.code = code;
    return this;
  }

  QRBuilder uri(URI uri) {
    this.uri = uri;
    return this;
  }

}
