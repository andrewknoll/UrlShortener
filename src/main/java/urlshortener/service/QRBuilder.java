package urlshortener.service;

import java.net.URI;
import java.util.function.BiFunction;

import urlshortener.domain.QR;

public class QRBuilder {

  private String hash;
  private String fileName;
  private URI uri;
  private byte[] code;

  static QRBuilder newInstance() {
    return new QRBuilder();
  }

  QR build() {
    return new QR(hash, fileName, uri, code);
  }

  QRBuilder hash(String hash) {
    this.hash = hash;
    return this;
  }

  QRBuilder fileName(String fileName) {
    if (fileName != null) {
      this.fileName = fileName;
    }
    else {
      this.fileName = hash + ".png";
    }
    
    return this;
  }

  QRBuilder code(byte[] code) {
    this.code = code;
    return this;
  }

  QRBuilder uri(BiFunction<String, String, URI> extractor) {
    this.uri = extractor.apply(hash, fileName);
    return this;
  }

}
