CREATE TABLE `forecast` (
  `id` bigint(8) NOT NULL AUTO_INCREMENT,
  `workflow` varchar(45) NOT NULL,
  `date_created` datetime NOT NULL,
  `last_updated` datetime NOT NULL,
  PRIMARY KEY (`id`)
);

CREATE TABLE `forecast_metadata` (
  `forecast_id` bigint(8) NOT NULL,
  `key` varchar(45) NOT NULL,
  `value` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`forecast_id`,`key`),
  CONSTRAINT `fk_metadata_forecast` FOREIGN KEY (`forecast_id`) REFERENCES `forecast` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `headcount_distribution` (
  `id` bigint(8) NOT NULL AUTO_INCREMENT,
  `forecast_id` bigint(8) NOT NULL,
  `process_name` varchar(45) DEFAULT NULL,
  `area` varchar(45) DEFAULT NULL,
  `quantity` int(11) DEFAULT NULL,
  `quantity_metric_unit` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_headcount_dist_forecast_idx` (`forecast_id`),
  CONSTRAINT `fk_headcount_dist_forecast` FOREIGN KEY (`forecast_id`) REFERENCES `forecast` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `headcount_productivity` (
  `id` bigint(8) NOT NULL AUTO_INCREMENT,
  `forecast_id` bigint(8) NOT NULL,
  `process_name` varchar(45) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `productivity` double DEFAULT NULL,
  `productivity_metric_unit` varchar(45) DEFAULT NULL,
  `ability_level` tinyint(2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_headcount_produ_forecast_idx` (`forecast_id`),
  CONSTRAINT `fk_headcount_produ_forecast` FOREIGN KEY (`forecast_id`) REFERENCES `forecast` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `planning_distribution` (
  `id` bigint(8) NOT NULL AUTO_INCREMENT,
  `forecast_id` bigint(8) NOT NULL,
  `date_in` datetime DEFAULT NULL,
  `date_out` datetime DEFAULT NULL,
  `quantity` int(11) DEFAULT NULL,
  `quantity_metric_unit` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_planning_dist_forecast_idx` (`forecast_id`),
  CONSTRAINT `fk_planning_dist_forecast` FOREIGN KEY (`forecast_id`) REFERENCES `forecast` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `planning_distribution_metadata` (
  `planning_distribution_id` bigint(8) NOT NULL,
  `key` varchar(45) NOT NULL,
  `value` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`planning_distribution_id`,`key`),
  CONSTRAINT `fk_metadata_planning_dist` FOREIGN KEY (`planning_distribution_id`) REFERENCES `planning_distribution` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE `processing_distribution` (
  `id` bigint(8) NOT NULL AUTO_INCREMENT,
  `forecast_id` bigint(8) NOT NULL,
  `process_name` varchar(45) DEFAULT NULL,
  `type` varchar(45) DEFAULT NULL,
  `date` datetime DEFAULT NULL,
  `quantity` int(11) DEFAULT NULL,
  `quantity_metric_unit` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_processing_dist_forecast_idx` (`forecast_id`),
  CONSTRAINT `fk_processing_dist_forecast` FOREIGN KEY (`forecast_id`) REFERENCES `forecast` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
