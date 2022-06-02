CREATE TABLE IF NOT EXISTS `inputs_optimization`
(
  `warehouse_id`    VARCHAR(10) NOT NULL,
  `domain`          VARCHAR(40) NOT NULL,
  `json_value`      JSON NOT NULL,
  PRIMARY KEY (`warehouse_id`, `domain`)
);
