CREATE TABLE `current_forecast_deviation` (
	`id` bigint(8) NOT NULL AUTO_INCREMENT,
	`logistic_center_id` varchar(10) NOT NULL,
	`date_from` datetime NOT NULL,
    `date_to` datetime NOT NULL,
	`value` decimal(4,3) NOT NULL,
	`is_active` tinyint(4) NOT NULL,
	`workflow` varchar(45) NOT NULL,
    `user_id` BIGINT(20) NOT NULL,
  PRIMARY KEY (`id`)
)