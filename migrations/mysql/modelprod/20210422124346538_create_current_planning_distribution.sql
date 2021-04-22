CREATE TABLE `current_planning_distribution` (
    `id` BIGINT(8) NOT NULL,
    `workflow` VARCHAR(45) NOT NULL,
    `logistic_center_id` VARCHAR(45) NOT NULL,
    `date_out` DATETIME NOT NULL,
    `quantity` INT NOT NULL,
    `date_created` DATETIME NOT NULL,
    `last_updated` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `workflow_lc_date_idx` (`workflow` ASC, `logistic_center_id` ASC, `date_out` ASC) VISIBLE
);
