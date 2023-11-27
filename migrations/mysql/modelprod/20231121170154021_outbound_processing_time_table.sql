CREATE TABLE IF NOT EXISTS `outbound_processing_time`
(
    `id`                  BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT UNIQUE,
    `logistic_center_id`  VARCHAR(13)         NOT NULL,
    `etd_day`             VARCHAR(10)         NOT NULL,
    `etd_hour`            VARCHAR(6)          NOT NULL,
    `etd_processing_time` TINYINT(1) UNSIGNED NOT NULL,
    `date_created`        TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `date_updated`        TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_active`           TINYINT(1)          NOT NULL,
    PRIMARY KEY (`id`),
    INDEX processing_time_idx (`logistic_center_id`, `is_active`, `etd_day`, `etd_hour`)
);
