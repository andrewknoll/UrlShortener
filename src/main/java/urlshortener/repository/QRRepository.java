package urlshortener.repository;

import java.util.List;

import urlshortener.domain.QR;

public interface QRRepository {

  QR findByHash(String hash);

  QR save(QR qr);

  void update(QR qr);

  void delete(String id);

  Long count();

  List<QR> list(Long limit, Long offset);

}
