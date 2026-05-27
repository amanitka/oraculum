-- Flyway migration script for Spring Modulith event publication table
-- Required by spring-modulith-starter-jpa EventPublicationRegistry

CREATE TABLE IF NOT EXISTS public.event_publication (
    id                     UUID         NOT NULL PRIMARY KEY,
    listener_id            TEXT         NOT NULL,
    event_type             TEXT         NOT NULL,
    serialized_event       TEXT         NOT NULL,
    publication_date       TIMESTAMPTZ  NOT NULL,
    status                 VARCHAR(255),
    completion_date        TIMESTAMPTZ,
    completion_attempts    INTEGER      NOT NULL DEFAULT 0,
    last_resubmission_date TIMESTAMPTZ
);

CREATE INDEX ix_event_publication_status_pub_date
    ON public.event_publication (status, publication_date);
