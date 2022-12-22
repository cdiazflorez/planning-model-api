ALTER TABLE current_forecast_deviation ADD COLUMN type VARCHAR(10) NOT NULL DEFAULT 'units';
ALTER TABLE current_forecast_deviation ADD COLUMN path VARCHAR(30) DEFAULT NULL;