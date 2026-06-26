-- Moves refrigerant onto the compressor and adds ISCR (variable-speed) fields (PostgreSQL).
--
-- Refrigerant is now a property of the Compressor (selected in the add/edit compressor
-- form, after LRA); a unit's refrigerant is derived from its compressor. The old
-- unit.refrigerant_id column is left in place (harmless under ddl-auto: validate) and can
-- be dropped manually once no longer needed.
--
-- For ISCR compressors the admin also enters base/min/max RPM and second capacity and
-- power-input curves (q_c11..q_c20 and p_c11..p_c20). All nullable (non-ISCR leaves NULL).
--
-- Run ONCE before deploying the backend when ddl-auto is 'validate' (prod). With
-- ddl-auto: update (local) Hibernate adds the columns automatically and this is a no-op.

-- Refrigerant on the compressor
ALTER TABLE compressor ADD COLUMN IF NOT EXISTS refrigerant_id BIGINT;

-- ADD CONSTRAINT has no IF NOT EXISTS in PostgreSQL, so guard it to keep the script
-- re-runnable (idempotent).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_compressor_refrigerant'
    ) THEN
        ALTER TABLE compressor
            ADD CONSTRAINT fk_compressor_refrigerant
            FOREIGN KEY (refrigerant_id) REFERENCES refrigerant (id);
    END IF;
END $$;

-- ISCR RPMs on the compressor specs
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS rpm_base DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS rpm_min  DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS rpm_max  DOUBLE PRECISION;

-- Second capacity curve (Q11..Q20), ISCR only
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c11 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c12 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c13 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c14 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c15 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c16 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c17 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c18 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c19 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS q_c20 DOUBLE PRECISION;

-- Second power-input curve (P11..P20), ISCR only
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c11 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c12 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c13 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c14 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c15 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c16 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c17 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c18 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c19 DOUBLE PRECISION;
ALTER TABLE compressor_specs ADD COLUMN IF NOT EXISTS p_c20 DOUBLE PRECISION;
