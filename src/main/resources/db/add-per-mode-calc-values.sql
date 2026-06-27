-- Moves a ProjectDetails' calculation values from one-to-one to one-per-mode (PostgreSQL).
--
-- A heat pump's project detail now holds BOTH a COOLING and a HEATING row of inputs/outputs
-- under the single dual-mode PDF, instead of a single row + a `mod` on project_details. So:
--   * custom_calc_vals / calc_output_vals each gain a `mod` column and a `project_details_id` FK
--     (the child now points at its parent detail -> one-to-many).
--   * project_details drops its old one-to-one FK columns and its `mod` column.
--
-- Run ONCE. In prod (ddl-auto: validate) run the WHOLE script before deploying. Locally
-- (ddl-auto: update) Hibernate adds the new columns/FKs automatically, but it does NOT drop the
-- obsolete columns or relax the old NOT NULL on custom_calc_vals_id -- which would break inserts of
-- new project_details. So locally you must at least run steps 2 and 4 (backfill + drop) once.

-- 1. New columns: operating mode + parent link on each value table.
ALTER TABLE custom_calc_vals ADD COLUMN IF NOT EXISTS mod VARCHAR(16);
ALTER TABLE custom_calc_vals ADD COLUMN IF NOT EXISTS project_details_id BIGINT;
ALTER TABLE calc_output_vals ADD COLUMN IF NOT EXISTS mod VARCHAR(16);
ALTER TABLE calc_output_vals ADD COLUMN IF NOT EXISTS project_details_id BIGINT;

-- 2. Backfill the parent link + mode from the existing one-to-one columns on project_details.
--    Existing details predate dual-mode, so each maps to exactly one value row; default the mode
--    to COOLING where project_details.mod was never set.
UPDATE custom_calc_vals c
   SET project_details_id = pd.id,
       mod = COALESCE(pd.mod, 'COOLING')
  FROM project_details pd
 WHERE pd.custom_calc_vals_id = c.id;

UPDATE calc_output_vals o
   SET project_details_id = pd.id,
       mod = COALESCE(pd.mod, 'COOLING')
  FROM project_details pd
 WHERE pd.calc_output_vals_id = o.id;

-- 3. Foreign keys for the new one-to-many link (skip if Hibernate already created them locally).
ALTER TABLE custom_calc_vals
    ADD CONSTRAINT fk_custom_calc_vals_project_details
    FOREIGN KEY (project_details_id) REFERENCES project_details (id);

ALTER TABLE calc_output_vals
    ADD CONSTRAINT fk_calc_output_vals_project_details
    FOREIGN KEY (project_details_id) REFERENCES project_details (id);

-- 4. Drop the obsolete one-to-one columns and the per-detail mode. The old custom_calc_vals_id was
--    NOT NULL; leaving it would block inserts of new details, so it must go.
ALTER TABLE project_details DROP COLUMN IF EXISTS custom_calc_vals_id;
ALTER TABLE project_details DROP COLUMN IF EXISTS calc_output_vals_id;
ALTER TABLE project_details DROP COLUMN IF EXISTS mod;
