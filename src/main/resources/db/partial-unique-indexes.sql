-- Active-only uniqueness for soft-deletable components and units (PostgreSQL).
--
-- Soft delete sets deleted = true instead of removing rows, because existing units
-- reference component specs and saved-units / projects reference units. These PARTIAL
-- UNIQUE INDEXES enforce uniqueness only among ACTIVE (deleted = false) rows, so once
-- an item is deleted its model/code can be added again, while duplicate active rows
-- are still rejected at the database level.
--
-- Run ONCE, after the schema has been created (ddl-auto: update/create) and before
-- switching ddl-auto back to validate. Hibernate cannot generate partial indexes from
-- annotations, so this is the only way they get created.

CREATE UNIQUE INDEX IF NOT EXISTS uq_compressor_model_active
    ON compressor (model) WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_evaporator_model_active
    ON evaporator (model) WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_condenser_model_active
    ON condenser (model) WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_expansion_valve_model_active
    ON expansion_valve (model) WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_four_way_reversing_valve_model_active
    ON four_way_reversing_valve (model) WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_chassis_model_active
    ON chassis (model) WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_refrigerant_code_active
    ON refrigerant (code) WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_unit_model_active
    ON unit (brand, series, model) WHERE deleted = false;
