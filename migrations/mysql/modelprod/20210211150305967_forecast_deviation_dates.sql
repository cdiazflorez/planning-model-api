ALTER TABLE `current_forecast_deviation`
ADD COLUMN `date_created` DATETIME NOT NULL DEFAULT NOW() AFTER `user_id`,
ADD COLUMN `last_updated` DATETIME NOT NULL DEFAULT NOW() AFTER `date_created`;