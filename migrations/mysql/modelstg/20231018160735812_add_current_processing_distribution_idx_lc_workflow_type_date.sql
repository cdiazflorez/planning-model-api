ALTER TABLE current_processing_distribution
    DROP INDEX logistic_center_id;
CREATE INDEX current_processing_distribution_idx_lc_workflow_type_date
    USING BTREE ON current_processing_distribution (`logistic_center_id`, `workflow`, `type`, `date`) VISIBLE;
