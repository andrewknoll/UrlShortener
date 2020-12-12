package urlshortener.domain;

import java.net.URI;
import java.sql.Date;

public class ShortURL {

  private String hash;
  private String target;
  private URI uri;
  private String sponsor;
  private Date created;
  private String owner;
  private Integer mode;
  private volatile Boolean safe;
  private String ip;
  private String country;
  private URI qrUri;
  private volatile String description;

  public ShortURL(String hash, String target, URI uri, String sponsor,
                  Date created, String owner, Integer mode, Boolean safe, String ip,
                  String country, URI qrUri, String description) {
    this.hash = hash;
    this.target = target;
    this.uri = uri;
    this.sponsor = sponsor;
    this.created = created;
    this.owner = owner;
    this.mode = mode;
    this.safe = safe;
    this.ip = ip;
    this.country = country;
    this.qrUri = qrUri;
    this.description = description;
  }

  public ShortURL() {
  }

  public String getDescription() {
    return description;
  }

  public String getHash() {
    return hash;
  }

  public String getTarget() {
    return target;
  }

  public URI getUri() {
    return uri;
  }

  public Date getCreated() {
    return created;
  }

  public String getOwner() {
    return owner;
  }

  public Integer getMode() {
    return mode;
  }

  public String getSponsor() {
    return sponsor;
  }

  public Boolean getSafe() {
    return safe;
  }

  public String getIP() {
    return ip;
  }

  public String getCountry() {
    return country;
  }

  public URI getqrUri() {
    return qrUri;
  }

  public void setDescription(String desc) {
    this.description = desc;
  }

  public void setSafe(boolean safe) {
    this.safe = safe;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }
}
