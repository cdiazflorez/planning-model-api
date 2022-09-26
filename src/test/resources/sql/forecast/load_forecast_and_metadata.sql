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

-- there is no deviation

