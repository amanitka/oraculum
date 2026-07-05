CREATE TABLE public.t_user (
    id                     BIGSERIAL    PRIMARY KEY,
    email                  VARCHAR(255) NOT NULL UNIQUE,
    first_name             VARCHAR(255),
    last_name              VARCHAR(255),
    provider               VARCHAR(50) NOT NULL,  -- 'keycloak' or 'google'
    role                   VARCHAR(20) NOT NULL,
    analysis_limit         VARCHAR(10),  -- e.g. '5D', '10W', '20M'; NULL = unlimited
    enabled                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at          TIMESTAMPTZ
);
