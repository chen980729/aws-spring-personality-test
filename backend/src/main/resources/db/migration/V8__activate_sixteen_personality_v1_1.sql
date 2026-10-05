-- Activate DefinitionVersion 1.1 after the compatibility release has been deployed.
-- Retire 1.0 first so the one-AVAILABLE-version unique index is never violated.
-- Existing Sessions remain bound to their original DefinitionVersion and continue
-- to execute with that version's immutable semantics.

DO $$
DECLARE
    retired_count integer;
    promoted_count integer;
BEGIN
    UPDATE assessment_definition_versions
    SET status = 'RETIRED'
    WHERE definition_id = '6a0c8f0d-4f8a-4b8e-9d2d-7d5d0fa2b3c1'
      AND version_code = '1.0'
      AND status = 'AVAILABLE';

    GET DIAGNOSTICS retired_count = ROW_COUNT;

    IF retired_count <> 1 THEN
        RAISE EXCEPTION
            'Expected exactly one AVAILABLE SIXTEEN_PERSONALITY 1.0 row to retire, updated %',
            retired_count;
    END IF;

    UPDATE assessment_definition_versions
    SET status = 'AVAILABLE',
        published_at = TIMESTAMPTZ '2026-10-05 23:00:00+00'
    WHERE definition_id = '6a0c8f0d-4f8a-4b8e-9d2d-7d5d0fa2b3c1'
      AND version_code = '1.1'
      AND status = 'DRAFT';

    GET DIAGNOSTICS promoted_count = ROW_COUNT;

    IF promoted_count <> 1 THEN
        RAISE EXCEPTION
            'Expected exactly one DRAFT SIXTEEN_PERSONALITY 1.1 row to promote, updated %',
            promoted_count;
    END IF;
END
$$;
