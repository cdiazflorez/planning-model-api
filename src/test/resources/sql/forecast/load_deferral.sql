INSERT INTO outbound_deferral_data(id, logistic_center_id, date, cpt, status, updated)
VALUES
    (1, 'ARTW01', '2022-09-08 10:00:00', '2022-09-08 12:00:00', 'CAP_MAX', true),
    (2, 'ARTW01', '2022-09-08 11:00:00', '2022-09-08 13:00:00', 'CASCADE', true),
    (3, 'ARTW01', '2022-09-08 18:00:00', '2022-09-09 20:00:00', 'CASCADE', true),
    (4, 'ARTW01', '2022-09-08 18:00:00', '2022-09-09 20:00:00', 'CASCADE', false),
    (5, 'COTW01', '2022-09-10 10:00:00', '2022-09-08 12:00:00', 'CAP_MAX', true),
    (6, 'COTW01', '2022-09-10 11:00:00', '2022-09-08 13:00:00', 'CASCADE', true),
    (7, 'COTW01', '2022-09-11 18:00:00', '2022-09-09 20:00:00', 'CAP_MAX', true),
    (8, 'COTW01', '2022-09-11 18:00:00', '2022-09-09 20:00:00', 'NOT_DEFERRED', true);
