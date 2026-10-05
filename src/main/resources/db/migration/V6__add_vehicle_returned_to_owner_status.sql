ALTER TABLE vehicle
    DROP CONSTRAINT vehicle_status_check,
    ADD CONSTRAINT ck_vehicle_status
        CHECK (status IN ('RECEIVED', 'IN_PREPARATION', 'AVAILABLE', 'RESERVED', 'SOLD', 'RETURNED_TO_OWNER'));

ALTER TABLE vehicle_status_history
    DROP CONSTRAINT vehicle_status_history_previous_status_check,
    DROP CONSTRAINT vehicle_status_history_new_status_check,
    ADD CONSTRAINT ck_vehicle_status_history_previous_status
        CHECK (previous_status IN ('RECEIVED', 'IN_PREPARATION', 'AVAILABLE', 'RESERVED', 'SOLD', 'RETURNED_TO_OWNER')),
    ADD CONSTRAINT ck_vehicle_status_history_new_status
        CHECK (new_status IN ('RECEIVED', 'IN_PREPARATION', 'AVAILABLE', 'RESERVED', 'SOLD', 'RETURNED_TO_OWNER'));
