CREATE TABLE legal_categories (
    id          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(100) NOT NULL,
    description text,
    is_active   boolean      NOT NULL DEFAULT true,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT legal_categories_name_key UNIQUE (name)
);

-- Reference data: there is no admin UI in the MVP, so categories are managed through migrations
INSERT INTO legal_categories (name, description) VALUES
    ('Criminal Law', 'Criminal charges, defense, and related proceedings'),
    ('Employment Law', 'Workplace disputes and employment issues'),
    ('Family Law', 'Divorce, custody, and other family matters'),
    ('Immigration Law', 'Visas, residency, citizenship, and deportation matters'),
    ('Personal Injury', 'Accidents, injuries, and compensation claims'),
    ('Property Law', 'Real estate, land, and landlord-tenant matters');
