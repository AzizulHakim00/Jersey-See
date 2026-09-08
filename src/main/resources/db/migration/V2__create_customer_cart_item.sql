CREATE TABLE customer_cart_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    line_id VARCHAR(36) NOT NULL,
    customer_id BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    printing_type VARCHAR(32) NOT NULL,
    printing_name VARCHAR(50) NULL,
    printing_number VARCHAR(2) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customer_cart_item_line_id UNIQUE (line_id),
    CONSTRAINT fk_customer_cart_item_customer
        FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_cart_item_variant
        FOREIGN KEY (product_variant_id) REFERENCES product_variant (id) ON DELETE CASCADE,
    CONSTRAINT chk_customer_cart_item_quantity CHECK (quantity BETWEEN 1 AND 10),
    INDEX idx_customer_cart_item_customer (customer_id),
    INDEX idx_customer_cart_item_variant (product_variant_id)
) ENGINE=InnoDB;
