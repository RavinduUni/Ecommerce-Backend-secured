-- Schema as Hibernate created it, for setting up a NEW empty database.
-- On your existing database Flyway skips this file (baseline-on-migrate) and
-- records it as already applied.

CREATE TABLE user_address (
    id       BIGINT NOT NULL AUTO_INCREMENT,
    street   VARCHAR(255),
    city     VARCHAR(255),
    state    VARCHAR(255),
    country  VARCHAR(255),
    zipcode  VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE user_entity (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    email       VARCHAR(255),
    password    VARCHAR(255),
    phone       VARCHAR(255),
    role        VARCHAR(20),
    address_id  BIGINT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_user_entity_address UNIQUE (address_id),
    CONSTRAINT fk_user_entity_address FOREIGN KEY (address_id) REFERENCES user_address (id)
) ENGINE = InnoDB;

CREATE TABLE products (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    name            VARCHAR(255),
    description     VARCHAR(255),
    price           DECIMAL(38, 2),
    stock_quantity  INT,
    category        VARCHAR(255),
    image_url       VARCHAR(255),
    image_file_id   VARCHAR(255),
    active          BIT,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE cart_item_entity (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT,
    price       DECIMAL(38, 2),
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_item_user FOREIGN KEY (user_id) REFERENCES user_entity (id),
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB;

CREATE TABLE order_entity (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    total_amount  DECIMAL(38, 2),
    status        VARCHAR(20),
    created_at    DATETIME(6),
    updated_at    DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES user_entity (id)
) ENGINE = InnoDB;

CREATE TABLE order_item_entity (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    order_id    BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT,
    price       DECIMAL(38, 2),
    PRIMARY KEY (id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES order_entity (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB;