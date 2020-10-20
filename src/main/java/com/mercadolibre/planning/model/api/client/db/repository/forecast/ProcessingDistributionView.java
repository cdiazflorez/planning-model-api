package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;

import java.time.ZonedDateTime;

public interface ProcessingDistributionView {

    long getId();

    ZonedDateTime getDate();

    ProcessName getProcessName();

    long getQuantity();

    MetricUnit getQuantityMetricUnit();

    ProcessingType getType();

}
