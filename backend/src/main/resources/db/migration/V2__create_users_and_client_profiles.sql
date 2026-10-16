CREATE TABLE users (
    id             uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    email          varchar(254) NOT NULL,
    password_hash  varchar(255) NOT NULL,
    role           varchar(20)  NOT NULL DEFAULT 'CLIENT',
    status         varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    email_verified boolean      NOT NULL DEFAULT false,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    deleted_at     timestamptz,
    CONSTRAINT users_role_check CHECK (role IN ('CLIENT', 'LAWYER', 'ADMIN')),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DISABLED'))
);

-- Emails are unique regardless of case
CREATE UNIQUE INDEX users_email_lower_key ON users (lower(email));

CREATE TABLE client_profiles (
    id         uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid         NOT NULL REFERENCES users (id),
    first_name varchar(100) NOT NULL,
    last_name  varchar(100) NOT NULL,
    phone      varchar(30),
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT client_profiles_user_id_key UNIQUE (user_id)
);
