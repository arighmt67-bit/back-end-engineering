CREATE TABLE notification_log (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    ticket_id BIGINT NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    message VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_notification_log_event_id UNIQUE (event_id)
);

CREATE INDEX idx_notification_log_ticket_id ON notification_log (ticket_id);
