CREATE TABLE photo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT,
    file_name VARCHAR(255),
    file_url VARCHAR(500),
    description VARCHAR(500),
    is_deleted BOOLEAN,

    -- photo'ya özgü alanlar burada
    exif_id BIGINT
);