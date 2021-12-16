ALTER TABLE planning_distribution ADD KEY USING BTREE (forecast_id, date_out), ADD KEY USING BTREE (forecast_id, date_in);

ALTER TABLE current_forecast_deviation ADD KEY USING BTREE (logistic_center_id, date_to);

ALTER TABLE current_processing_distribution ADD KEY USING BTREE (logistic_center_id, date);

ALTER TABLE current_headcount_productivity ADD KEY USING BTREE (logistic_center_id, date);

ALTER table `processing_distribution`
    ADD KEY USING BTREE (forecast_id, date),
    DROP KEY fk_processing_dist_forecast_idx;

ALTER table `headcount_productivity`
    ADD KEY USING BTREE (forecast_id, date),
    DROP KEY fk_headcount_produ_forecast_idx;
