INSERT INTO outbound_processing_time (logistic_center_id,
                                      etd_day,
                                      etd_hour,
                                      etd_processing_time,
                                      date_created,
                                      date_updated,
                                      is_active)
VALUES ('ARBA01', 'monday', '2200', 4, '2023-09-17 12:30:00', '2023-11-17 18:40:00', false),
       ('ARBA01', 'monday', '2200', 3, '2023-11-17 18:40:00', '2023-11-17 18:40:00', true),
       ('COCU01', 'thursday', '2000', 2, '2023-11-17 22:30:00', '2023-11-17 22:30:00', true),
       ('COCU01', 'friday', '1530', 3, '2023-11-16 16:00:00', '2023-11-17 12:30:00', true);

