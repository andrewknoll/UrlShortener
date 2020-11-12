package urlshortener.repository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.HSQL;
import static urlshortener.fixtures.QRFixture.badqr;
import static urlshortener.fixtures.QRFixture.qr1;
import static urlshortener.fixtures.QRFixture.qr1modified;
import static urlshortener.fixtures.QRFixture.qr2;
import static urlshortener.fixtures.QRFixture.qr3;
import static urlshortener.fixtures.ShortURLFixture.badUrl;
import static urlshortener.fixtures.ShortURLFixture.url1;
import static urlshortener.fixtures.ShortURLFixture.url1modified;
import static urlshortener.fixtures.ShortURLFixture.url2;
import static urlshortener.fixtures.ShortURLFixture.url3;

import java.io.File;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import urlshortener.domain.QR;
import urlshortener.repository.impl.QRRepositoryImpl;
import urlshortener.repository.impl.ShortURLRepositoryImpl;

public class QRRepositoryTests {

  private EmbeddedDatabase db;
  private QRRepository repository;
  private ShortURLRepository shortURLrep;
  private JdbcTemplate jdbc;

  @Before
  public void setup() {
    db = new EmbeddedDatabaseBuilder().setType(HSQL)
        .addScript("schema-hsqldb.sql").build();
    jdbc = new JdbcTemplate(db);
    repository = new QRRepositoryImpl(jdbc);
    shortURLrep = new ShortURLRepositoryImpl(jdbc);
  }

  @Test
  public void thatSavePersistsTheQR() {
    shortURLrep.save(url1());
    assertNotNull(repository.save(qr1()));
    assertSame(jdbc.queryForObject("select count(*) from QRCode",
        Integer.class), 1);
  }

  @Test
  public void thatSaveFile() {
    shortURLrep.save(url3());
    assertNotNull(repository.save(qr3()));
    assertArrayEquals(jdbc.queryForObject("select image from QRCode",
        byte[].class), qr3().getQR());
  }

  @Test
  public void thatSaveFilename() {
    shortURLrep.save(url2());
    assertNotNull(repository.save(qr2()));
    assertSame(
        jdbc.queryForObject("select filename from QRCode", String.class),
        "file2.png");
  }

  @Test
  public void thatSaveADuplicateHashIsSafelyIgnored() {
    shortURLrep.save(url1());
    repository.save(qr1());
    assertNotNull(repository.save(qr1()));
    assertSame(jdbc.queryForObject("select count(*) from QRCode",
        Integer.class), 1);
  }

  @Test
  public void thatMissingShortURLInSaveReturnsNull() {
    assertNull(repository.save(badqr()));
    assertSame(jdbc.queryForObject("select count(*) from QRCode", Integer.class), 0);
  }
  @Test
  public void thatFindByHashReturnsAQR() {
    shortURLrep.save(url1());
    shortURLrep.save(url2());
    repository.save(qr1());
    repository.save(qr2());
    QR qr = repository.findByHash(qr1().getHash());
    assertNotNull(qr);
    assertSame(qr.getHash(), qr1().getHash());
  }

  @Test
  public void thatFindByNameReturnsAQR() {
    shortURLrep.save(url1());
    shortURLrep.save(url2());
    repository.save(qr1());
    repository.save(qr2());
    QR qr = repository.findByName(qr1().getFileName());
    assertNotNull(qr);
    assertSame(qr.getHash(), qr1().getHash());
  }

  @Test
  public void thatFindByHashReturnsNullWhenFails() {
    shortURLrep.save(url1());
    repository.save(qr1());
    assertNull(repository.findByHash(qr2().getHash()));
  }

  @Test
  public void thatFindByNameReturnsNullWhenFails() {
    shortURLrep.save(url1());
    repository.save(qr1());
    assertNull(repository.findByName(qr2().getFileName()));
  }

  @Test
  public void thatDeleteDelete() {
    shortURLrep.save(url1());
    shortURLrep.save(url2());
    repository.save(qr1());
    repository.save(qr2());
    repository.delete(qr1().getHash());
    assertEquals(repository.count().intValue(), 1);
    repository.delete(qr2().getHash());
    assertEquals(repository.count().intValue(), 0);
  }

  @Test
  public void thatUpdateUpdate() {
    shortURLrep.save(url1());
    repository.save(qr1());
    QR qr = repository.findByHash(qr1().getHash());
    assertArrayEquals(jdbc.queryForObject("select image from QRCode",
        byte[].class), qr.getQR());
    repository.update(qr1modified());
    
    qr = repository.findByHash(qr1().getHash());
    assertArrayEquals(jdbc.queryForObject("select image from QRCode",
        byte[].class), qr.getQR());
  }

  @After
  public void shutdown() {
    db.shutdown();
  }

}
