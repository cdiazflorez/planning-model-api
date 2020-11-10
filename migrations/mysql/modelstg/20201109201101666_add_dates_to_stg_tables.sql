ALTER TABLE `modelstg`.`current_headcount_productivity`
ADD COLUMN `date_created` DATETIME NOT NULL AFTER `is_active`,
ADD COLUMN `last_updated` DATETIME NOT NULL AFTER `date_created`;

ALTER TABLE `modelstg`.`current_processing_distribution`
ADD COLUMN `date_created` DATETIME NOT NULL AFTER `is_active`,
ADD COLUMN `last_updated` DATETIME NOT NULL AFTER `date_created`;

ALTER TABLE `modelstg`.`configuration`
ADD COLUMN `date_created` DATETIME NOT NULL AFTER `metric_unit`,
ADD COLUMN `last_updated` DATETIME NOT NULL AFTER `date_created`;
