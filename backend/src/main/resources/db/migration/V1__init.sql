CREATE TABLE destinations (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120)  NOT NULL,
    target_url    VARCHAR(2048) NOT NULL,
    source        VARCHAR(60)   NOT NULL,
    secret        VARCHAR(255),
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_destinations_source ON destinations (source);

CREATE TABLE webhook_events (
    id            BIGSERIAL PRIMARY KEY,
    event_id      VARCHAR(120)  NOT NULL UNIQUE,
    source        VARCHAR(60)   NOT NULL,
    event_type    VARCHAR(120)  NOT NULL,
    payload       TEXT          NOT NULL,
    headers       TEXT,
    status        VARCHAR(30)   NOT NULL,
    received_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_source ON webhook_events (source);
CREATE INDEX idx_events_status ON webhook_events (status);

CREATE TABLE delivery_attempts (
    id                BIGSERIAL PRIMARY KEY,
    event_id          BIGINT      NOT NULL REFERENCES webhook_events (id) ON DELETE CASCADE,
    destination_id    BIGINT      NOT NULL REFERENCES destinations (id) ON DELETE CASCADE,
    attempt_number    INT         NOT NULL,
    status            VARCHAR(30) NOT NULL,
    response_status   INT,
    response_body     TEXT,
    error_message     TEXT,
    duration_ms       BIGINT,
    attempted_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attempts_event ON delivery_attempts (event_id);
CREATE INDEX idx_attempts_status ON delivery_attempts (status);
