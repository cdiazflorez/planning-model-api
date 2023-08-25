CREATE INDEX idx_forecast_workflow_lc_week_date USING BTREE
    ON forecast (`workflow`,`logistic_center_id`,`week`,`date_created`) VISIBLE;
