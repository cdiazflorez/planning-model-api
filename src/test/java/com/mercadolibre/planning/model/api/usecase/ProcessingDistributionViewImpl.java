package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;

import java.util.Date;

import static com.mercadolibre.planning.model.api.util.DateUtils.fromDate;

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

    @Override
    public EntityOutput toEntityOutput(final Workflow workflow, final Source source) {
        return EntityOutput.builder()
                .workflow(workflow)
                .date(fromDate(getDate()))
                .processName(getProcessName())
                .value(getQuantity())
                .metricUnit(getQuantityMetricUnit())
                .type(getType())
                .source(source)
                .build();
    }
}
