ALTER TABLE vehicle
    ADD COLUMN chassis VARCHAR(30) NOT NULL,
    ADD COLUMN plate   VARCHAR(7)  NOT NULL,
    ADD COLUMN mileage INTEGER     NOT NULL CHECK (mileage >= 0),
    ADD CONSTRAINT uk_vehicle_chassis UNIQUE (chassis),
    ADD CONSTRAINT uk_vehicle_plate UNIQUE (plate);
