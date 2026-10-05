ALTER TABLE assessment_dimension_tie_breaks
    ADD COLUMN question_id TEXT,
    ADD COLUMN selected_option_id TEXT;

ALTER TABLE assessment_dimension_tie_breaks
    ADD CONSTRAINT ck_assessment_dimension_tie_breaks_contextual_pair
        CHECK (
            (
                question_id IS NULL
                    AND selected_option_id IS NULL
            )
            OR
            (
                question_id IS NOT NULL
                    AND BTRIM(question_id) <> ''
                    AND selected_option_id IS NOT NULL
                    AND BTRIM(selected_option_id) <> ''
            )
        );
