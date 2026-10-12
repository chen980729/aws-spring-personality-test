ALTER TABLE assessment_clarification_runtime_messages
    ADD CONSTRAINT ck_assessment_clarification_runtime_messages_length
        CHECK (
            (
                message_role = 'ASSISTANT_QUESTION'
                AND char_length(message_text) <= 500
            )
            OR
            (
                message_role = 'USER_ANSWER'
                AND char_length(message_text) <= 1000
            )
        );
