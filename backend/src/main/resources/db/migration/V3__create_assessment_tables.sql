CREATE TABLE assessment_definitions (
                                        id UUID NOT NULL,
                                        code TEXT NOT NULL,
                                        name TEXT NOT NULL,
                                        created_at TIMESTAMPTZ NOT NULL,

                                        CONSTRAINT pk_assessment_definitions
                                            PRIMARY KEY (id),

                                        CONSTRAINT uq_assessment_definitions_code
                                            UNIQUE (code)
);


CREATE TABLE assessment_definition_versions (
                                                id UUID NOT NULL,
                                                definition_id UUID NOT NULL,
                                                version_code TEXT NOT NULL,
                                                status TEXT NOT NULL,
                                                specification JSONB NOT NULL,
                                                created_at TIMESTAMPTZ NOT NULL,
                                                published_at TIMESTAMPTZ,

                                                CONSTRAINT pk_assessment_definition_versions
                                                    PRIMARY KEY (id),

                                                CONSTRAINT fk_assessment_definition_versions_definition
                                                    FOREIGN KEY (definition_id)
                                                        REFERENCES assessment_definitions(id)
                                                        ON DELETE RESTRICT,

                                                CONSTRAINT uq_assessment_definition_versions_definition_version
                                                    UNIQUE (definition_id, version_code),

                                                CONSTRAINT uq_assessment_definition_versions_id_definition
                                                    UNIQUE (id, definition_id),

                                                CONSTRAINT ck_assessment_definition_versions_status
                                                    CHECK (
                                                        status IN ('DRAFT', 'AVAILABLE', 'RETIRED')
                                                        ),

                                                CONSTRAINT ck_assessment_definition_versions_specification_object
                                                    CHECK (
                                                        jsonb_typeof(specification) = 'object'
                                                        ),

                                                CONSTRAINT ck_assessment_definition_versions_publication
                                                    CHECK (
                                                        (status = 'DRAFT' AND published_at IS NULL)
                                                            OR
                                                        (status IN ('AVAILABLE', 'RETIRED') AND published_at IS NOT NULL)
                                                        )
);

CREATE UNIQUE INDEX uq_assessment_definition_versions_available
    ON assessment_definition_versions(definition_id)
    WHERE status = 'AVAILABLE';

CREATE TABLE assessment_sessions (
                                     id UUID NOT NULL,
                                     user_id UUID NOT NULL,
                                     definition_id UUID NOT NULL,
                                     definition_version_id UUID NOT NULL,
                                     status TEXT NOT NULL,

                                     questionnaire_response JSONB,
                                     questionnaire_submitted_at TIMESTAMPTZ,

                                     initial_result JSONB,
                                     final_result JSONB,

                                     created_at TIMESTAMPTZ NOT NULL,
                                     completed_at TIMESTAMPTZ,
                                     abandoned_at TIMESTAMPTZ,

                                     version BIGINT NOT NULL,

                                     CONSTRAINT pk_assessment_sessions
                                         PRIMARY KEY (id),

                                     CONSTRAINT fk_assessment_sessions_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES user_accounts(id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT fk_assessment_sessions_definition
                                         FOREIGN KEY (definition_id)
                                             REFERENCES assessment_definitions(id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT fk_assessment_sessions_definition_version
                                         FOREIGN KEY (definition_version_id, definition_id)
                                             REFERENCES assessment_definition_versions(id, definition_id)
                                             ON DELETE RESTRICT,

                                     CONSTRAINT ck_assessment_sessions_status
                                         CHECK (
                                             status IN (
                                                        'IN_PROGRESS',
                                                        'AWAITING_CLARIFICATION',
                                                        'CLARIFICATION_IN_PROGRESS',
                                                        'COMPLETED',
                                                        'ABANDONED'
                                                 )
                                             ),

                                     CONSTRAINT ck_assessment_sessions_questionnaire_response_object
                                         CHECK (
                                             questionnaire_response IS NULL
                                                 OR jsonb_typeof(questionnaire_response) = 'object'
                                             ),

                                     CONSTRAINT ck_assessment_sessions_initial_result_object
                                         CHECK (
                                             initial_result IS NULL
                                                 OR jsonb_typeof(initial_result) = 'object'
                                             ),

                                     CONSTRAINT ck_assessment_sessions_final_result_object
                                         CHECK (
                                             final_result IS NULL
                                                 OR jsonb_typeof(final_result) = 'object'
                                             ),

                                     CONSTRAINT ck_assessment_sessions_submission_pair
                                         CHECK (
                                             (
                                                 questionnaire_submitted_at IS NULL
                                                     AND initial_result IS NULL
                                                 )
                                                 OR
                                             (
                                                 questionnaire_submitted_at IS NOT NULL
                                                     AND initial_result IS NOT NULL
                                                 )
                                             ),

                                     CONSTRAINT ck_assessment_sessions_submitted_response
                                         CHECK (
                                             questionnaire_submitted_at IS NULL
                                                 OR questionnaire_response IS NOT NULL
                                             ),

                                     CONSTRAINT ck_assessment_sessions_lifecycle
                                         CHECK (
                                             (
                                                 status = 'IN_PROGRESS'
                                                     AND questionnaire_submitted_at IS NULL
                                                     AND initial_result IS NULL
                                                     AND final_result IS NULL
                                                     AND completed_at IS NULL
                                                     AND abandoned_at IS NULL
                                                 )
                                                 OR
                                             (
                                                 status IN (
                                                            'AWAITING_CLARIFICATION',
                                                            'CLARIFICATION_IN_PROGRESS'
                                                     )
                                                     AND questionnaire_submitted_at IS NOT NULL
                                                     AND initial_result IS NOT NULL
                                                     AND final_result IS NULL
                                                     AND completed_at IS NULL
                                                     AND abandoned_at IS NULL
                                                 )
                                                 OR
                                             (
                                                 status = 'COMPLETED'
                                                     AND questionnaire_submitted_at IS NOT NULL
                                                     AND initial_result IS NOT NULL
                                                     AND final_result IS NOT NULL
                                                     AND completed_at IS NOT NULL
                                                     AND abandoned_at IS NULL
                                                 )
                                                 OR
                                             (
                                                 status = 'ABANDONED'
                                                     AND final_result IS NULL
                                                     AND completed_at IS NULL
                                                     AND abandoned_at IS NOT NULL
                                                 )
                                             )
);

CREATE UNIQUE INDEX uq_assessment_sessions_active
    ON assessment_sessions(user_id, definition_id)
    WHERE status IN (
        'IN_PROGRESS',
        'AWAITING_CLARIFICATION',
        'CLARIFICATION_IN_PROGRESS'
    );

CREATE INDEX ix_assessment_sessions_completed_history
    ON assessment_sessions(user_id, completed_at DESC)
    WHERE status = 'COMPLETED';

CREATE TABLE assessment_dimension_clarifications (
                                                     id UUID NOT NULL,
                                                     session_id UUID NOT NULL,
                                                     dimension_code TEXT NOT NULL,
                                                     status TEXT NOT NULL,

                                                     result_outcome TEXT,
                                                     suggested_pole TEXT,
                                                     result_confidence TEXT,
                                                     result_summary TEXT,

                                                     ai_provider TEXT,
                                                     ai_model_identifier TEXT,
                                                     clarification_policy_revision TEXT,

                                                     started_at TIMESTAMPTZ,
                                                     accepted_at TIMESTAMPTZ,
                                                     updated_at TIMESTAMPTZ NOT NULL,

                                                     CONSTRAINT pk_assessment_dimension_clarifications
                                                         PRIMARY KEY (id),

                                                     CONSTRAINT fk_assessment_dimension_clarifications_session
                                                         FOREIGN KEY (session_id)
                                                             REFERENCES assessment_sessions(id)
                                                             ON DELETE CASCADE,

                                                     CONSTRAINT uq_assessment_dimension_clarifications_session_dimension
                                                         UNIQUE (session_id, dimension_code),

                                                     CONSTRAINT ck_assessment_dimension_clarifications_status
                                                         CHECK (
                                                             status IN (
                                                                        'PENDING',
                                                                        'IN_PROGRESS',
                                                                        'CLARIFIED',
                                                                        'SKIPPED',
                                                                        'FAILED_RETRYABLE'
                                                                 )
                                                             ),

                                                     CONSTRAINT ck_assessment_dimension_clarifications_result_outcome
                                                         CHECK (
                                                             result_outcome IS NULL
                                                                 OR result_outcome IN ('RESOLVED', 'UNCLEAR')
                                                             ),

                                                     CONSTRAINT ck_assessment_dimension_clarifications_accepted_result
                                                         CHECK (
                                                             (
                                                                 status = 'CLARIFIED'
                                                                     AND result_outcome IS NOT NULL
                                                                     AND result_confidence IS NOT NULL
                                                                     AND ai_provider IS NOT NULL
                                                                     AND ai_model_identifier IS NOT NULL
                                                                     AND clarification_policy_revision IS NOT NULL
                                                                     AND accepted_at IS NOT NULL
                                                                     AND (
                                                                     (
                                                                         result_outcome = 'RESOLVED'
                                                                             AND suggested_pole IS NOT NULL
                                                                         )
                                                                         OR
                                                                     (
                                                                         result_outcome = 'UNCLEAR'
                                                                             AND suggested_pole IS NULL
                                                                         )
                                                                     )
                                                                 )
                                                                 OR
                                                             (
                                                                 status <> 'CLARIFIED'
                                                                     AND result_outcome IS NULL
                                                                     AND suggested_pole IS NULL
                                                                     AND result_confidence IS NULL
                                                                     AND result_summary IS NULL
                                                                     AND ai_provider IS NULL
                                                                     AND ai_model_identifier IS NULL
                                                                     AND clarification_policy_revision IS NULL
                                                                     AND accepted_at IS NULL
                                                                 )
                                                             )
);

CREATE UNIQUE INDEX uq_assessment_dimension_clarifications_in_progress
    ON assessment_dimension_clarifications(session_id)
    WHERE status = 'IN_PROGRESS';

CREATE TABLE assessment_dimension_tie_breaks (
                                                 session_id UUID NOT NULL,
                                                 dimension_code TEXT NOT NULL,
                                                 selected_pole TEXT NOT NULL,
                                                 decided_at TIMESTAMPTZ NOT NULL,

                                                 CONSTRAINT pk_assessment_dimension_tie_breaks
                                                     PRIMARY KEY (session_id, dimension_code),

                                                 CONSTRAINT fk_assessment_dimension_tie_breaks_session
                                                     FOREIGN KEY (session_id)
                                                         REFERENCES assessment_sessions(id)
                                                         ON DELETE CASCADE
);