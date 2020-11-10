ALTER TABLE `current_headcount_productivity`
ADD COLUMN `date_created` DATETIME NOT NULL AFTER `is_active`,
ADD COLUMN `last_updated` DATETIME NOT NULL AFTER `date_created`;

ALTER TABLE `current_processing_distribution`
ADD COLUMN `date_created` DATETIME NOT NULL AFTER `is_active`,
ADD COLUMN `last_updated` DATETIME NOT NULL AFTER `date_created`;

ALTER TABLE `configuration`
ADD COLUMN `date_created` DATETIME NOT NULL AFTER `metric_unit`,
ADD COLUMN `last_updated` DATETIME NOT NULL AFTER `date_created`;
