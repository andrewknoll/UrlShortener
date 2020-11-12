package urlshortener.repository.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.hsqldb.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.stereotype.Repository;
import urlshortener.domain.QR;
import urlshortener.repository.QRRepository;

@Repository
public class QRRepositoryImpl implements QRRepository {

  private static final Logger log = LoggerFactory
      .getLogger(QRRepositoryImpl.class);

  private static final RowMapper<QR> rowMapper =
      (rs, rowNum) -> new QR(rs.getString("hash"), rs.getString("filename"),
          null, rs.getBytes("image"));

  private final JdbcTemplate jdbc;

  public QRRepositoryImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public QR findByName(String id) {
    try {
      return jdbc.queryForObject("SELECT * FROM QRCODE WHERE filename=?", rowMapper, id);
    } catch (Exception e) {
      log.debug("When select for key {}", id, e);
      return null;
    }
  }
  
  @Override
  public QR findByHash(String id) {
    try {
      return jdbc.queryForObject("SELECT * FROM QRCODE WHERE hash=?",
          rowMapper, id);
    } catch (Exception e) {
      log.debug("When select for key {}", id, e);
      return null;
    }
  }

  @Override
  public QR save(QR qr) {
    try {
      jdbc.update("INSERT INTO QRCODE VALUES (?,?,?)",
          new Object[] {qr.getHash(), qr.getFileName(), 
              blobify(qr.getQR()) },
          new int[] { Types.VARCHAR, Types.VARCHAR, Types.BLOB });
    } catch (DuplicateKeyException e) {
      log.debug("When insert for key {}", qr.getHash(), e);
      return qr;
    } catch (Exception e) {
      log.debug("When insert", e);
      return null;
    }
    return qr;
  }

  @Override
  public void update(QR su) {
    try {
      jdbc.update(
          "update QRCODE set filename=? image=? where hash=?",
          su.getFileName(), su.getQR(), su.getHash());
    } catch (Exception e) {
      log.debug("When update for hash {}", su.getHash(), e);
    }
  }

  @Override
  public void delete(String hash) {
    try {
      jdbc.update("delete from QRCODE where hash=?", hash);
    } catch (Exception e) {
      log.debug("When delete for hash {}", hash, e);
    }
  }

  @Override
  public Long count() {
    try {
      return jdbc.queryForObject("select count(*) from QRCODE",
          Long.class);
    } catch (Exception e) {
      log.debug("When counting", e);
    }
    return -1L;
  }

  @Override
  public List<QR> list(Long limit, Long offset) {
    try {
      return jdbc.query("SELECT * FROM QRCODE LIMIT ? OFFSET ?", new Object[] { limit, offset }, rowMapper);
    } catch (Exception e) {
      log.debug("When select for limit {} and offset {}", limit, offset, e);
      return Collections.emptyList();
    }
  }
  
  private SqlLobValue blobify(byte[] f) throws Exception{
    final InputStream is = new ByteArrayInputStream(f);
    LobHandler lobHandler = new DefaultLobHandler();
    return new SqlLobValue(is, (int)f.length, lobHandler);
  }

}
