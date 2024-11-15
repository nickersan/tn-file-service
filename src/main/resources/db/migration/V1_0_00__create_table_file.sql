CREATE TABLE IF NOT EXISTS file
(
    file_key          VARCHAR(500) NOT NULL PRIMARY KEY,
    content_type      VARCHAR(100) NOT NULL,
    name              VARCHAR(100) NOT NULL,
    size              INT          NOT NULL,
    data              BLOB         NOT NULL,
    created           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);