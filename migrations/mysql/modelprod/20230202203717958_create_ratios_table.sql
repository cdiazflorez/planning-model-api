create table `ratios`
(
    `id`                 BIGINT(8) NOT NULL AUTO_INCREMENT,
    `workflow`           VARCHAR(45)   NOT NULL,
    `logistic_center_id` VARCHAR(45)   NOT NULL,
    `type`               VARCHAR(20)   NOT NULL,
    `date`               DATETIME      NOT NULL,
    `value`              decimal(4, 3) NOT NULL,
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX                logistic_center_date_idx (`logistic_center_id`, `type`, `date`)
);