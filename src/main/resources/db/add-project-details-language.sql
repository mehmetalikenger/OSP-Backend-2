-- Adds the report-language column to project_details (PostgreSQL).
--
-- The add-to-project flow now records which language ("en"/"de") each stored report PDF
-- was rendered in, so regenerating the report after a project edit keeps the same language.
--
-- Run ONCE before deploying the backend when ddl-auto is 'validate' (prod). With
-- ddl-auto: update (local) Hibernate adds the column automatically and this is a no-op.
-- Existing rows keep NULL, which the app treats as English ("en").

ALTER TABLE project_details ADD COLUMN IF NOT EXISTS language VARCHAR(8);
