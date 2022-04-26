create index idx_processing_distribution_date_type
    on processing_distribution (date, type) using BTREE;