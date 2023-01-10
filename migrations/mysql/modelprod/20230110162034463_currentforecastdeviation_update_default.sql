alter table current_forecast_deviation alter type set default 'UNITS';
update current_forecast_deviation set type = 'UNITS' where type = 'units';