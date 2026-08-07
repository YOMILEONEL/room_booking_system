CREATE TABLE invoices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_number VARCHAR(40) NOT NULL,
    payment_id BIGINT NOT NULL,
    customer_username_snapshot VARCHAR(255) NOT NULL,
    room_name_snapshot VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    invoice_date DATE NOT NULL,
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_invoices_invoice_number UNIQUE (invoice_number),
    CONSTRAINT uk_invoices_payment_id UNIQUE (payment_id),
    CONSTRAINT fk_invoices_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
) ENGINE=InnoDB;
