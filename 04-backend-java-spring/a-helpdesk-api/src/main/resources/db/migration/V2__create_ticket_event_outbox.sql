CREATE TABLE ticket_event_outbox (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    event_version INTEGER NOT NULL,
    ticket_id BIGINT NOT NULL,
    owner_email VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_ticket_event_outbox_pending
    ON ticket_event_outbox (published_at, occurred_at);
