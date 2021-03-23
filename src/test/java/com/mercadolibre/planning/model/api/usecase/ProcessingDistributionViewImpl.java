package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Date;

@Builder
@AllArgsConstructor
public class ProcessingDistributionViewImpl implements ProcessingDistributionView {

    private final long id;

    private final Date date;

    private final ProcessName processName;

    private final long quantity;

    private final MetricUnit quantityMetricUnit;

    private final ProcessingType type;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Date getDate() {
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
