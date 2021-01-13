package urlshortener.repository.impl;

import java.util.Calendar;
import java.util.Hashtable;

public class SponsorCache {
    private static volatile SponsorCache instance;
    private volatile static Hashtable<String, SponsorCache.ExpirableSponsor> cache;

    private SponsorCache() {
        cache = new Hashtable<String, SponsorCache.ExpirableSponsor>();
    }

    public static SponsorCache getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (SponsorCache.class) {
            if (instance == null) {
                instance = new SponsorCache();
            }
            return instance;
        }
    }

    public String find(String sponsor) {
        SponsorCache.ExpirableSponsor esp = cache.get(sponsor);
        if (esp != null) {
            return esp.getPage();
        }

        return null;
    }

    public String put(String sponsor, String value) {
        cache.put(sponsor, SponsorCache.ExpirableSponsor.create(value));
        return value;
    }

    private static class ExpirableSponsor {
        private String page;
        private Calendar expiryDate;

        private ExpirableSponsor(String page, Calendar expiryDate) {
            this.page = page;
            this.expiryDate = expiryDate;
        }

        public String getPage() {
            Calendar now = Calendar.getInstance();
            if (expiryDate.before(now)) {
                return page;
            }

            return null;
        }

        public static SponsorCache.ExpirableSponsor create(String page) {
            Calendar expiryDate = Calendar.getInstance();
            expiryDate.setLenient(true);
            expiryDate.add(Calendar.DAY_OF_MONTH, 365);
            return new SponsorCache.ExpirableSponsor(page,  expiryDate);
        }
    }
}
