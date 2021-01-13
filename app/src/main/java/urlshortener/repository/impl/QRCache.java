package urlshortener.repository.impl;

import java.util.Calendar;
import java.util.Hashtable;

import urlshortener.domain.QR;

public class QRCache {
    private static volatile QRCache instance;
    private volatile static Hashtable<String, ExpirableQR> cache;

    private QRCache() {
        cache = new Hashtable<String, ExpirableQR>();
    }

    public static QRCache getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (QRCache.class) {
            if (instance == null) {
                instance = new QRCache();
            }
            return instance;
        }
    }

    public QR find(String hash) {
        ExpirableQR eqr = cache.get(hash);
        if (eqr != null) {
            return eqr.getImage();
        }
        
        return null;
    }

    public QR put(String hash, QR value) {
        cache.put(hash, ExpirableQR.create(value));
        return value;
    }

    private static class ExpirableQR {
        private QR image;
        private Calendar expiryDate;

        private ExpirableQR(QR image, Calendar expiryDate) {
            this.image = image;
            this.expiryDate = expiryDate;
        }

        public QR getImage() {
            Calendar now = Calendar.getInstance();
            if (expiryDate.before(now)) {
                return image;
            }

            return null;
        }

        public static ExpirableQR create(QR image) {
            Calendar expiryDate = Calendar.getInstance();
            expiryDate.setLenient(true);
            expiryDate.add(Calendar.DAY_OF_MONTH, 10);
            return new ExpirableQR(image,  expiryDate);
        } 
    }
}
