CREATE TABLE user_accounts (
                               id UUID NOT NULL,
                               email TEXT NOT NULL,
                               display_name TEXT NOT NULL,
                               password_hash TEXT NOT NULL,
                               created_at TIMESTAMPTZ NOT NULL,
                               version BIGINT NOT NULL,

                               CONSTRAINT pk_user_accounts
                                   PRIMARY KEY (id),

                               CONSTRAINT uq_user_accounts_email
                                   UNIQUE (email)
);