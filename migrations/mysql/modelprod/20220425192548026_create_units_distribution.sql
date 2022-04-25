CREATE TABLE `units_distribution`
(
    `id`                   bigint(8) NOT NULL AUTO_INCREMENT,
    `date`                 datetime    NOT NULL,
    `logistic_center_id`   varchar(45) NOT NULL,
    `workflow`             varchar(45) NOT NULL,
    `process_name`         varchar(45) NOT NULL,
    `area`                 varchar(45) NOT NULL,
    `quantity`             bigint(8) NOT NULL,
    `quantity_metric_unit` varchar(45) NOT NULL,
    PRIMARY KEY (`id`)
);

create index idx_units_distribution_date_logistic_center_id
    on units_distribution (date, logistic_center_id) using BTREE;