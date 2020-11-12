-- Clean database

DROP TABLE CLICK IF EXISTS;
<<<<<<< HEAD
DROP TABLE QRCODE IF EXISTS;
=======
>>>>>>> 4dde2d1b43e3d03b02507cf1b337e9d80eef27cf
DROP TABLE SHORTURL IF EXISTS;

-- ShortURL

CREATE TABLE SHORTURL
(
    HASH    VARCHAR(30) PRIMARY KEY, -- Key
    TARGET  VARCHAR(1024),           -- Original URL
    SPONSOR VARCHAR(1024),           -- Sponsor URL
    CREATED TIMESTAMP,               -- Creation date
    OWNER   VARCHAR(255),            -- User id
    MODE    INTEGER,                 -- Redirect mode
    SAFE    BOOLEAN,                 -- Safe target
    IP      VARCHAR(20),             -- IP
    COUNTRY VARCHAR(50)              -- Country
);

-- QRCode

CREATE TABLE QRCODE
(
    HASH      VARCHAR(10) PRIMARY KEY NOT NULL FOREIGN KEY REFERENCES SHORTURL (HASH), -- Foreign and primary key
    FILENAME  VARCHAR(100),                                                            -- FileName (if provided)
    IMAGE     BLOB(5K) NOT NULL                                                        -- QR code image
);

-- Click

CREATE TABLE CLICK
(
    ID       BIGINT IDENTITY,                                             -- KEY
    HASH     VARCHAR(10) NOT NULL FOREIGN KEY REFERENCES SHORTURL (HASH), -- Foreing key
    CREATED  TIMESTAMP,                                                   -- Creation date
    REFERRER VARCHAR(1024),                                               -- Traffic origin
    BROWSER  VARCHAR(50),                                                 -- Browser
    PLATFORM VARCHAR(50),                                                 -- Platform
    IP       VARCHAR(20),                                                 -- IP
    COUNTRY  VARCHAR(50)                                                  -- Country
)