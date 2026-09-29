ALTER TABLE assessment_dimension_clarifications
    ADD COLUMN active_execution_token UUID;

-- Existing IN_PROGRESS rows may have been created before execution-token
-- coordination existed. They cannot have a live token from the old code,
-- so use the clarification id as a one-time opaque migration value.
UPDATE assessment_dimension_clarifications
SET active_execution_token = id
WHERE status = 'IN_PROGRESS';

ALTER TABLE assessment_dimension_clarifications
    ADD CONSTRAINT ck_assessment_dimension_clarifications_execution_token
        CHECK (
            (
                status = 'IN_PROGRESS'
                    AND active_execution_token IS NOT NULL
            )
            OR
            (
                status <> 'IN_PROGRESS'
                    AND active_execution_token IS NULL
            )
        );
