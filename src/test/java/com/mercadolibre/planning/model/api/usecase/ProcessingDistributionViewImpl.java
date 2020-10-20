package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import lombok.AllArgsConstructor;

import java.time.ZonedDateTime;

@AllArgsConstructor
public class ProcessingDistributionViewImpl implements ProcessingDistributionView {

    private long id;

    private ZonedDateTime date;

    private ProcessName processName;

    private long quantity;

    private MetricUnit quantityMetricUnit;

    private ProcessingType type;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public ZonedDateTime getDate() {
        return date;
    }

    @Override
    public ProcessName getProcessName() {
        return processName;
    }

    @Override
    public long getQuantity() {
        return quantity;
    }

    @Override
    public MetricUnit getQuantityMetricUnit() {
        return quantityMetricUnit;
    }

    @Override
    public ProcessingType getType() {
        return type;
    }
}
