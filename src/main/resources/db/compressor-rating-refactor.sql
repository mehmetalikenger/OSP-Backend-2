-- Consolidates compressor performance data onto CompressorRating and removes the legacy
-- CompressorSpecs / UnitCalculationEngine path (PostgreSQL).
--
-- New model:
--   * CompressorRating (compressor + refrigerant + coef arrays) is the single coef set, one per
--     (compressor, refrigerant) — created by the frdata importer (Frascold) or the admin (Copeland).
--   * compressor_mode_capacity holds admin-entered nominal (capacity, power input) per mode
--     (COOLING/HEATING), one-to-many under a rating. Frascold rows start with NONE (the export has
--     no capacity data) — admins add them later in the edit page.
--   * tech_specs references compressor_rating only; its own capacity/max_capacity are dropped (a
--     unit-mode's capacity now comes from the rating's mode-capacity for that mode).
--   * Refrigerant moves off compressor onto the rating.
--
-- Run ONCE. Prod (ddl-auto: validate): run the whole script before deploying. Local
-- (ddl-auto: update): Hibernate creates compressor_mode_capacity automatically, but does NOT drop
-- the obsolete NOT NULL columns/tables, so steps 2-4 must be run once or new inserts will fail.

-- 1. New per-mode capacity table (auto-created locally by ddl-auto: update).
CREATE SEQUENCE IF NOT EXISTS osp_comp_mode_cap_sequence START 1 INCREMENT 50;
CREATE TABLE IF NOT EXISTS compressor_mode_capacity (
    id                    BIGINT PRIMARY KEY,
    compressor_rating_id  BIGINT NOT NULL REFERENCES compressor_rating (id),
    mod                   VARCHAR(16) NOT NULL,
    capacity              DOUBLE PRECISION NOT NULL,
    power_input           DOUBLE PRECISION NOT NULL,
    max_capacity          DOUBLE PRECISION,
    CONSTRAINT uq_comp_mode_cap UNIQUE (compressor_rating_id, mod)
);

-- 2. tech_specs: capacity now lives on the rating's mode-capacity; the legacy specs link is gone.
ALTER TABLE tech_specs DROP COLUMN IF EXISTS capacity;
ALTER TABLE tech_specs DROP COLUMN IF EXISTS max_capacity;
ALTER TABLE tech_specs DROP COLUMN IF EXISTS compressor_specs_id;

-- 3. Refrigerant moves off the compressor (it's a property of the rating).
ALTER TABLE compressor DROP COLUMN IF EXISTS refrigerant_id;

-- 4. Drop the legacy specs table (replaced by compressor_rating + compressor_mode_capacity).
DROP TABLE IF EXISTS compressor_specs;
