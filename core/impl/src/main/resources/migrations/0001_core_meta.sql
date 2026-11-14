-- Core baseline: key/value metadata table owned by the Voltexa core.
-- (meta_key/meta_value: "key" and "value" are reserved words in H2 2.x)
CREATE TABLE IF NOT EXISTS voltexa_meta (
    meta_key VARCHAR(128) PRIMARY KEY,
    meta_value VARCHAR(4096) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
