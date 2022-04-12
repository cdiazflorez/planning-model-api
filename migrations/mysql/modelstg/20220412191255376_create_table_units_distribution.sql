CREATE TABLE `units_distribution` (
    `id` bigint(8) NOT NULL AUTO_INCREMENT,
    `area` varchar(45) NOT NULL,
    `date` datetime NOT NULL,
    `logistic_center_id` varchar(45) NOT NULL,
    `process_name` varchar(45) NOT NULL,
    `quantity` bigint(8) NOT NULL,
    `quantity_metric_unit` varchar(45) NOT NULL,
     PRIMARY KEY (`id`)
);
