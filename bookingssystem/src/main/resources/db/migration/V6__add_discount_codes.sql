CREATE TABLE discount_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_discount_codes_code UNIQUE (code)
) ENGINE=InnoDB;

ALTER TABLE payments ADD COLUMN applied_discount_code VARCHAR(80);
