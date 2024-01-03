alter table processing_distribution
    add column resource           varchar(45) default null,
    add column tags               text        default null,
    add column workflow           varchar(45) default null,
    add column logistic_center_id varchar(13) default null;