CREATE TABLE IF NOT EXISTS `outbound_deferral_data`
(
    `id`                 BIGINT(8) NOT NULL AUTO_INCREMENT,
    `logistic_center_id` VARCHAR(6)  NOT NULL,
    `date`               DATETIME    NOT NULL,
    `cpt`                DATETIME    NOT NULL,
    `status`             VARCHAR(15) NOT NULL,
    `updated`            TINYINT(1) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX                logistic_center_date_idx (`logistic_center_id`, `date`, `updated`)
);
