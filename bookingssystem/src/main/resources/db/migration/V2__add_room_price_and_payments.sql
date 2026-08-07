ALTER TABLE room ADD COLUMN price_per_night DECIMAL(10,2) NOT NULL DEFAULT 0;

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    booking_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6),
    paid_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_payments_booking_id UNIQUE (booking_id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES booking (booking_id)
) ENGINE=InnoDB;
