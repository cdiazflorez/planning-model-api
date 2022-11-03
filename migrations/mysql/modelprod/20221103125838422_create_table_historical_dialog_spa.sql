CREATE TABLE `historical_dialog_spa`
(
    `id`              bigint(11) NOT NULL AUTO_INCREMENT,
    `request_date`    datetime   NOT NULL,
    `spa_request`     json       NOT NULL,
    `spa_response`    json       NOT NULL,
    `front_result`    json DEFAULT NULL,
    `response_date`   datetime   NOT NULL,
    `logistic_center` varchar(9) NOT NULL,
    PRIMARY KEY (`id`)
);
