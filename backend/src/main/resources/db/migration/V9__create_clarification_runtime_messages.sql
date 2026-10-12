CREATE TABLE assessment_clarification_runtime_messages (
    clarification_id UUID NOT NULL,
    execution_token UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    message_role TEXT NOT NULL,
    message_text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_assessment_clarification_runtime_messages
        PRIMARY KEY (
            clarification_id,
            execution_token,
            sequence_number
        ),

    CONSTRAINT fk_assessment_clarification_runtime_messages_clarification
        FOREIGN KEY (clarification_id)
            REFERENCES assessment_dimension_clarifications(id)
            ON DELETE CASCADE,

    CONSTRAINT ck_assessment_clarification_runtime_messages_sequence
        CHECK (sequence_number > 0),

    CONSTRAINT ck_assessment_clarification_runtime_messages_role
        CHECK (
            message_role IN (
                'ASSISTANT_QUESTION',
                'USER_ANSWER'
            )
        ),

    CONSTRAINT ck_assessment_clarification_runtime_messages_text
        CHECK (btrim(message_text) <> ''),

    CONSTRAINT ck_assessment_clarification_runtime_messages_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT ck_assessment_clarification_runtime_messages_role_sequence
        CHECK (
            (
                message_role = 'ASSISTANT_QUESTION'
                AND MOD(sequence_number, 2) = 1
            )
            OR
            (
                message_role = 'USER_ANSWER'
                AND MOD(sequence_number, 2) = 0
            )
        )
);

CREATE INDEX ix_assessment_clarification_runtime_messages_expiry
    ON assessment_clarification_runtime_messages(expires_at);
