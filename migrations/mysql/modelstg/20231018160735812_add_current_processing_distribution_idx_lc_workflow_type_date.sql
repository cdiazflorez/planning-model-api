ALTER TABLE current_processing_distribution DROP INDEX logistic_center_id;
CREATE INDEX logistic_center_id
    USING BTREE ON current_processing_distribution (`logistic_center_id`, `workflow`, `type`, `date`) VISIBLE;

