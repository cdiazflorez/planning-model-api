ALTER TABLE `current_headcount_productivity`
ADD COLUMN `user_id` BIGINT(20) NULL DEFAULT NULL AFTER `last_updated`;

ALTER TABLE `current_processing_distribution`
ADD COLUMN `user_id` BIGINT(20) NULL DEFAULT NULL AFTER `last_updated`;
