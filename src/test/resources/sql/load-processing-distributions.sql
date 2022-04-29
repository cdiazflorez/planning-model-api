insert into forecast(workflow, date_created, last_updated, user_id)
values ('FBM_WMS_OUTBOUND', '2020-08-18 20:00:00', '2020-08-18 20:00:00', 1),
       ('FBM_WMS_OUTBOUND', '2020-08-18 20:00:00', '2020-08-18 20:00:00', 1),
       ('FBM_WMS_OUTBOUND', '2020-08-18 21:00:00', '2020-08-18 20:00:00', 1);

insert into forecast_metadata(forecast_id, "key", "value")
values (1, 'warehouse_id', 'ARTW01'),
       (1, 'week', '1'),
       (2, 'warehouse_id', 'ARTW02'),
       (2, 'week', '1'),
       (3, 'warehouse_id', 'ARTW02'),
       (3, 'week', '2');

insert into processing_distribution(forecast_id, process_name, type, date, quantity, quantity_metric_unit)
values (1, 'PICKING', 'PERFORMED_PROCESSING', '2020-08-19 17:00:00', 10, 'UNITS'),
       (1, 'PICKING', 'PERFORMED_PROCESSING', '2020-08-19 18:00:00', 20, 'UNITS'),
       (1, 'PICKING', 'PERFORMED_PROCESSING', '2020-08-19 19:00:00', 30, 'UNITS'),
       (2, 'PICKING', 'PERFORMED_PROCESSING', '2020-08-19 17:00:00', 01, 'UNITS'),
       (2, 'PICKING', 'PERFORMED_PROCESSING', '2020-08-19 18:00:00', 02, 'UNITS'),
       (3, 'PICKING', 'PERFORMED_PROCESSING', '2020-08-19 18:00:00', 20, 'UNITS'),
       (3, 'PICKING', 'PERFORMED_PROCESSING', '2020-08-19 19:00:00', 30, 'UNITS');
