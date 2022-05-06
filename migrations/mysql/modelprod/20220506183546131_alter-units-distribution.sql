truncate units_distribution;

ALTER table units_distribution MODIFY COLUMN quantity double(8,3) NOT NULL;