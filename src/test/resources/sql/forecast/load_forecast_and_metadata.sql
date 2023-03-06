INSERT INTO forecast(id, workflow, date_created, last_updated, user_id)
VALUES (1, 'FBM_WMS_OUTBOUND', '2022-09-08 12:31:00', '2022-09-08 12:31:00', 100),
       (2, 'FBM_WMS_OUTBOUND', '2022-09-08 12:31:00', '2022-09-08 12:31:00', 100),
       (3, 'FBM_WMS_OUTBOUND', '2022-09-08 12:32:00', '2022-09-08 12:31:00', 100),
       (4, 'FBM_WMS_OUTBOUND', '2022-09-08 12:30:00', '2022-09-08 12:31:00', 100),
       (5, 'FBM_WMS_OUTBOUND', '2022-09-08 12:30:00', '2022-09-08 12:31:00', 100),
       (6, 'FBM_WMS_OUTBOUND', '2022-09-08 12:30:00', '2022-09-08 12:31:00', 100),
       (7, 'FBM_WMS_OUTBOUND', '2022-09-08 12:30:00', '2022-09-08 12:31:00', 100);

INSERT INTO forecast_metadata(forecast_id, "key", "value")
VALUES (1, 'warehouse_id', 'ARTW01'),
       (1, 'week', '33-2022'),
       (2, 'warehouse_id', 'ARTW01'),
       (2, 'week', '34-2022'),
       (3, 'warehouse_id', 'ARTW01'),
       (3, 'week', '34-2022'),
       (4, 'warehouse_id', 'ARTW01'),
       (4, 'week', '35-2022'),
       (5, 'warehouse_id', 'ARTW01'),
       (5, 'week', '35-2022'),
       (6, 'warehouse_id', 'ARTW02'),
       (6, 'week', '34-2022'),
       (7, 'warehouse_id', 'ARTW02'),
       (7, 'week', '35-2022');

INSERT INTO current_forecast_deviation(id, logistic_center_id, date_from, date_to, "value", is_active, workflow, user_id, date_created, last_updated)
VALUES
    (1, 'ARTW01', '2022-09-08 12:30:00', '2022-09-09 12:40:00', 0.7, 0, 'FBM_WMS_OUTBOUND', 0, '2022-09-08 12:30:00', '2022-09-08 12:45:00'), -- there is no deviation as it was deactivated before view_date
    (2, 'ARTW01', '2022-09-08 12:30:00', '2022-09-09 12:40:00', 0.7, 0, 'FBM_WMS_OUTBOUND', 0, '2022-09-08 13:15:00', '2022-09-08 13:15:00'),

    (3, 'ARTW02', '2022-09-08 12:30:00', '2022-09-09 12:30:00', 0.5, 0, 'FBM_WMS_OUTBOUND', 1, '2022-09-08 12:30:00', '2022-09-08 12:30:00'), -- there is exactly one deviation that is still active

    (4, 'ARTW03', '2022-09-08 12:30:00', '2022-09-09 12:30:00', 0.5, 0, 'FBM_WMS_OUTBOUND', 0, '2022-09-08 12:30:00', '2022-09-08 14:30:00'), -- there is exactly one deviation that was deactivated

    (5, 'ARTW04', '2022-09-08 12:30:00', '2022-09-09 12:40:00', 0.7, 0, 'FBM_WMS_OUTBOUND', 0, '2022-09-08 12:30:00', '2022-09-08 12:59:00'),
    (6, 'ARTW04', '2022-09-08 12:30:00', '2022-09-09 12:30:00', 0.5, 0, 'FBM_WMS_OUTBOUND', 0, '2022-09-08 12:59:00', '2022-09-08 13:30:00'), -- there is more than one deviation
    (7, 'ARTW04', '2022-09-08 12:30:00', '2022-09-09 12:40:00', 0.7, 0, 'FBM_WMS_OUTBOUND', 1, '2022-09-08 13:30:00', '2022-09-08 12:30:00');


INSERT INTO current_headcount_productivity(id, date, logistic_center_id, workflow, process_name, productivity, productivity_metric_unit, ability_level, is_active, date_created, last_updated, user_id)
VALUES (1, '2022-09-08 11:30:00', 'ARTW01', 'FBM_WMS_OUTBOUND', 'PICKING', 10, 'UNITS_PER_HOUR', 0, 1, '2022-09-08 10:00:00', '2022-09-08 10:00:00', 1), -- there are simulations out of date range
       (2, '2022-09-08 14:30:00', 'ARTW01', 'FBM_WMS_OUTBOUND', 'PACKING', 10, 'UNITS_PER_HOUR', 0, 1, '2022-09-08 10:00:00', '2022-09-08 10:00:00', 1),

       (3, '2022-09-08 12:30:00', 'ARTW02', 'FBM_WMS_OUTBOUND', 'BATCH_SORTER', 10, 'UNITS_PER_HOUR', 0, 1, '2022-09-08 10:00:00', '2022-09-08 10:00:00', 1), -- there are simulations for other processes
       (4, '2022-09-08 12:30:00', 'ARTW02', 'FBM_WMS_OUTBOUND', 'WALL_IN', 10, 'UNITS_PER_HOUR', 0, 1, '2022-09-08 10:00:00', '2022-09-08 10:00:00', 1),

       (5, '2022-09-08 12:30:00', 'ARTW03', 'FBM_WMS_OUTBOUND', 'PICKING', 10, 'UNITS_PER_HOUR', 0, 1, '2022-09-08 10:15:00', '2022-09-08 10:15:00', 1), -- there is an active simulation
       (6, '2022-09-08 12:30:00', 'ARTW03', 'FBM_WMS_OUTBOUND', 'PACKING', 10, 'UNITS_PER_HOUR', 0, 1, '2022-09-08 10:15:00', '2022-09-08 10:15:00', 1),

       (7, '2022-09-08 12:30:00', 'ARTW04', 'FBM_WMS_OUTBOUND', 'PACKING', 09, 'UNITS_PER_HOUR', 0, 0, '2022-09-08 10:10:00', '2022-09-08 10:15:00', 1), -- the simulation was deactivated with another simulation
       (8, '2022-09-08 12:30:00', 'ARTW04', 'FBM_WMS_OUTBOUND', 'PACKING', 10, 'UNITS_PER_HOUR', 0, 1, '2022-09-08 10:15:00', '2022-09-08 10:15:00', 1),
       (9, '2022-09-08 12:30:00', 'ARTW04', 'FBM_WMS_OUTBOUND', 'PICKING', 10, 'UNITS_PER_HOUR', 0, 0, '2022-09-08 10:15:00', '2022-09-08 10:45:00', 1),
       (10, '2022-09-08 12:30:00', 'ARTW04', 'FBM_WMS_OUTBOUND', 'PICKING', 09, 'UNITS_PER_HOUR', 0, 1, '2022-09-08 10:45:00', '2022-09-08 10:45:00', 1);


INSERT INTO current_processing_distribution(id, date, logistic_center_id, workflow, process_path, process_name, quantity, quantity_metric_unit, "type", is_active, date_created, last_updated, user_id)
VALUES (1, '2022-09-08 11:30:00', 'ARTW01', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'PICKING', 10, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 1, '2022-09-08 10:00:00', '2022-09-08 10:00:00', 1), -- there are simulations out of date range
       (2, '2022-09-08 14:30:00', 'ARTW01', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'PACKING', 10, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 1, '2022-09-08 10:00:00', '2022-09-08 10:00:00', 1),

       (3, '2022-09-08 12:30:00', 'ARTW02', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'BATCH_SORTER', 10, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 1, '2022-09-08 10:00:00', '2022-09-08 10:00:00', 1), -- there are simulations for other processes
       (4, '2022-09-08 12:30:00', 'ARTW02', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'WALL_IN', 10, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 1, '2022-09-08 10:00:00', '2022-09-08 10:00:00', 1),

       (5, '2022-09-08 12:30:00', 'ARTW03', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'PICKING', 10, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 1, '2022-09-08 10:15:00', '2022-09-08 10:15:00', 1), -- there is an active simulation
       (6, '2022-09-08 12:30:00', 'ARTW03', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'PACKING', 10, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 1, '2022-09-08 10:15:00', '2022-09-08 10:15:00', 1),

       (7, '2022-09-08 12:30:00', 'ARTW04', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'PACKING', 09, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 0, '2022-09-08 10:10:00', '2022-09-08 10:15:00', 1), -- the simulation was deactivated with another simulation
       (8, '2022-09-08 12:30:00', 'ARTW04', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'PACKING', 10, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 1, '2022-09-08 10:15:00', '2022-09-08 10:15:00', 1),
       (9, '2022-09-08 12:30:00', 'ARTW04', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'PICKING', 10, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 0, '2022-09-08 10:15:00', '2022-09-08 10:45:00', 1),
       (10, '2022-09-08 12:30:00', 'ARTW04', 'FBM_WMS_OUTBOUND', 'GLOBAL', 'PICKING', 09, 'UNITS_PER_HOUR', 'EFFECTIVE_WORKERS', 1, '2022-09-08 10:45:00', '2022-09-08 10:45:00', 1);

INSERT INTO processing_distribution(id, forecast_id, process_path, process_name, type, date, quantity, quantity_metric_unit)
VALUES
    -- one forecast
    (1, 1, 'GLOBAL', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 10:00:00', 100, 'UNITS'),
    (2, 1, 'TOT_MONO', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 10:00:00', 70, 'UNITS'),
    (3, 1, 'TOT_MULTI_BATCH', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 10:00:00', 30, 'UNITS'),

    -- overlapping
    (4, 1, 'GLOBAL', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 100, 'UNITS'),
    (5, 1, 'TOT_MONO', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 70, 'UNITS'),
    (6, 1, 'TOT_MULTI_BATCH', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 30, 'UNITS'),

    (7, 2, 'GLOBAL', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 50, 'UNITS'),
    (8, 2, 'TOT_MONO', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 30, 'UNITS'),
    (9, 2, 'TOT_MULTI_BATCH', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 20, 'UNITS'),

    -- another forecast
    (10, 2, 'GLOBAL', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 12:00:00', 75, 'UNITS'),
    (11, 2, 'TOT_MONO', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 12:00:00', 5, 'UNITS'),
    (12, 2, 'NON_TOT_MONO', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 12:00:00', 70, 'UNITS'),

    -- from other forecasts
    (13, 3, 'GLOBAL', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 300, 'UNITS'),
    (14, 3, 'GLOBAL', 'PACKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 300, 'UNITS'),

    -- from other processes
    (15, 1, 'GLOBAL', 'PACKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 300, 'UNITS'),
    (16, 2, 'GLOBAL', 'PACKING', 'EFFECTIVE_WORKERS', '2022-09-08 11:00:00', 300, 'UNITS'),

    -- less than date_from
    (17, 1, 'GLOBAL', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 09:00:00', 300, 'UNITS'),
    (18, 1, 'TOT_MONO', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 09:00:00', 300, 'UNITS'),
    (19, 1, 'TOT_MULTI_BATCH', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 09:00:00', 300, 'UNITS'),

    -- greater than date_to
    (20, 2, 'GLOBAL', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 13:00:00', 300, 'UNITS'),
    (21, 2, 'TOT_MONO', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 13:00:00', 300, 'UNITS'),
    (22, 2, 'NON_TOT_MONO', 'PICKING', 'EFFECTIVE_WORKERS', '2022-09-08 13:00:00', 300, 'UNITS');
