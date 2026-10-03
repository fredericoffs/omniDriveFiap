ALTER TABLE vehicle
    ADD COLUMN current_sale_id UUID,
    ADD CONSTRAINT ck_vehicle_current_sale CHECK ((status IN ('RESERVED', 'SOLD')) = (current_sale_id IS NOT NULL));
