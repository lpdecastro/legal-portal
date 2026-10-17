CREATE TABLE cases (
    id           uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id    uuid         NOT NULL REFERENCES client_profiles (id),
    category_id  uuid         NOT NULL REFERENCES legal_categories (id),
    title        varchar(200) NOT NULL,
    description  text,
    location     varchar(255),
    status       varchar(20)  NOT NULL DEFAULT 'DRAFT',
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    submitted_at timestamptz,
    closed_at    timestamptz,
    deleted_at   timestamptz,
    CONSTRAINT cases_status_check CHECK (status IN ('DRAFT', 'SUBMITTED', 'CLOSED'))
);

-- Every case query is scoped to the current client
CREATE INDEX cases_client_id_idx ON cases (client_id);
CREATE INDEX cases_category_id_idx ON cases (category_id);
