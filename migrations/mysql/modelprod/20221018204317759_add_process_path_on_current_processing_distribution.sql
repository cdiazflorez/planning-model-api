ALTER TABLE `current_processing_distribution`
ADD COLUMN `process_path` varchar(45) NOT NULL DEFAULT 'GLOBAL';
