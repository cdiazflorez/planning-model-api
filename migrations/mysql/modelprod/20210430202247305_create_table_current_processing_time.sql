CREATE TABLE `current_processing_time` (
    `id` BIGINT(8) NOT NULL,
    `workflow` VARCHAR(45) NOT NULL,
    `logistic_center_id` VARCHAR(45) NOT NULL,
    `value` INT NOT NULL,
    `metric_unit` VARCHAR(45) NULL,
    `cpt_from` DATETIME NOT NULL,
    `cpt_to` DATETIME NOT NULL,
    `is_active` TINYINT(1) NOT NULL,
    `date_created` DATETIME NOT NULL,
    `last_updated` DATETIME NOT NULL,
    `user_id` BIGINT(20) NOT NULL,

    PRIMARY KEY (`id`)
);
