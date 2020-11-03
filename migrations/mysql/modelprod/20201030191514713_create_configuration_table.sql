CREATE TABLE `configuration` (
  `logistic_center_id` varchar(10) NOT NULL,
  `key` varchar(45) NOT NULL,
  `value` int(11) NOT NULL,
  `metric_unit` varchar(45) NOT NULL,
  PRIMARY KEY (`logistic_center_id`,`key`)
)
