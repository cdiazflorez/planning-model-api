CREATE TABLE `current_headcount_productivity` (
  `id` bigint(8) NOT NULL AUTO_INCREMENT,
  `workflow` varchar(45) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `logistic_center_id` varchar(45) DEFAULT NULL,
  `process_name` varchar(45) DEFAULT NULL,
  `productivity` int(11) DEFAULT NULL,
  `productivity_metric_unit` varchar(45) DEFAULT NULL,
  `ability_level` tinyint(2) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `current_processing_distribution` (
  `id` bigint(8) NOT NULL AUTO_INCREMENT,
  `workflow` varchar(45) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `logistic_center_id` varchar(45) DEFAULT NULL,
  `process_name` varchar(45) DEFAULT NULL,
  `quantity` bigint(8) DEFAULT NULL,
  `quantity_metric_unit` varchar(45) DEFAULT NULL,
  `type` varchar(45) DEFAULT NULL,
  `is_active` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`id`)
);
