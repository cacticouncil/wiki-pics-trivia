CREATE TABLE IF NOT EXISTS settings
(
    lang                    VARCHAR(8),
    sfx                     TINYINT,
    dnr                     TINYINT,
    learning                TINYINT,
    showAnswer              TINYINT,
    updates                 INTEGER,
    increment               INTEGER,
    help                    INTEGER,
    qotd                    TINYINT,
    qotd_hour               SMALLINT,
    qotd_minute             SMALLINT,
    music                   TINYINT
);

CREATE TABLE IF NOT EXISTS modes
(
    id                      INTEGER PRIMARY KEY,
    name                    VARCHAR,
    categories              SMALLINT,
    questions               SMALLINT,
    timer                   SMALLINT,
    misses                  SMALLINT,
    browsable               TINYINT,
    hints                   TINYINT,
    links                   TINYINT
);

CREATE TABLE IF NOT EXISTS themes
(
    id                      INTEGER PRIMARY KEY,
    name                    VARCHAR,
    color                   INTEGER
);

CREATE TABLE IF NOT EXISTS categories
(
    id                      INTEGER PRIMARY KEY,
    displayname             VARCHAR,
    name                    VARCHAR,
    theme                   INTEGER,
    FOREIGN KEY(theme)      REFERENCES themes(id)
);

CREATE TABLE IF NOT EXISTS properties
(
    id                      INTEGER PRIMARY KEY,
    name                    VARCHAR,
    question                VARCHAR,
    type                    TINYINT,
    unit                    TINYINT,
    filter                  TINYINT
);

CREATE TABLE IF NOT EXISTS topics
(
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    mid                     VARCHAR NOT NULL UNIQUE,
    wid                     VARCHAR,
    name                    VARCHAR,
    author                  VARCHAR,
    license                 VARCHAR
);

CREATE TABLE IF NOT EXISTS answers
(
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    text                    VARCHAR,
    used                    TINYINT,
    property                INTEGER,
    topic                   INTEGER,
    category                INTEGER,
    FOREIGN KEY(property)   REFERENCES properties(id),
    FOREIGN KEY(topic)      REFERENCES topics(id),
    FOREIGN KEY(category)   REFERENCES categories(id),
    CONSTRAINT uc_answer    UNIQUE(property, topic, text)
);

CREATE TABLE IF NOT EXISTS fake_answers
(
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    text                    VARCHAR,
    property                INTEGER,
    category                INTEGER,
    FOREIGN KEY(property)   REFERENCES properties(id),
    FOREIGN KEY(category)   REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS categories_to_properties
(
    id                      INTEGER PRIMARY KEY,
    category                INTEGER,
    property                INTEGER,
    FOREIGN KEY(category)   REFERENCES categories(id),
    FOREIGN KEY(property)   REFERENCES properties(id)
);

CREATE TABLE IF NOT EXISTS modes_to_categories
(
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    mode                    INTEGER,
    category                INTEGER,
    FOREIGN KEY(mode)       REFERENCES modes(id),
    FOREIGN KEY(category)   REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS blacklist
(
    term                    VARCHAR NOT NULL
);

CREATE TABLE IF NOT EXISTS category_stats
(
    id                      INTEGER PRIMARY KEY,
    cursor                  INTEGER
);

CREATE UNIQUE INDEX `idx_topics_mid` ON `topics` (`mid` ASC);
CREATE INDEX `idx_topics_wid` ON `topics` (`wid` ASC);