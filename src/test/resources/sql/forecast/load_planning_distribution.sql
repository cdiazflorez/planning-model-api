INSERT INTO forecast(id, workflow, date_created, last_updated, user_id)
VALUES (1, 'FBM_WMS_OUTBOUND', '2022-09-08 12:31:00', '2022-09-08 12:31:00', 100),
       (2, 'FBM_WMS_OUTBOUND', '2022-09-08 12:31:00', '2022-09-08 12:31:00', 100),
       (3, 'FBM_WMS_OUTBOUND', '2022-09-08 12:31:00', '2022-09-08 12:31:00', 100),
       (4, 'FBM_WMS_OUTBOUND', '2022-09-08 12:31:00', '2022-09-08 12:31:00', 100);

INSERT INTO planning_distribution(id, forecast_id, date_in, date_out, quantity, quantity_metric_unit, process_path)
VALUES (1, 1, '2022-11-09 10:00:00', '2022-11-11 10:00:00', 100.0, 'UNITS', 'TOT_MONO'),
       (2, 1, '2022-11-09 10:00:00', '2022-11-11 10:00:00', 200.0, 'UNITS', 'NON_TOT_MONO'),
       (3, 1, '2022-11-10 11:00:00', '2022-11-12 11:00:00', 50.0, 'UNITS', 'TOT_MONO'),
       (4, 1, '2022-11-10 11:00:00', '2022-11-12 11:00:00', 10.5, 'UNITS', 'NON_TOT_MONO'),
       (5, 1, '2022-11-10 11:00:00', '2022-11-12 11:00:00', 1000.0, 'UNITS', 'GLOBAL'),
       (6, 2, '2022-11-10 11:00:00', '2022-11-12 11:00:00', 100.0, 'UNITS', 'GLOBAL'),
       (7, 3, '2022-11-10 11:00:00', '2022-11-12 11:00:00', 45.6, 'UNITS', 'GLOBAL'),
       (8, 4, '2022-11-10 11:00:00', '2022-11-12 11:00:00', 1000.0, 'UNITS', 'GLOBAL');
