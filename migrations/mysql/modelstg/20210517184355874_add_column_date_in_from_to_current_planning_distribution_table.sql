ALTER TABLE `current_planning_distribution`
ADD COLUMN `date_in_from` DATETIME NOT NULL DEFAULT NOW();
