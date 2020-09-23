package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import lombok.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Value
public class ProcessingDistributionRequest {

    @NotNull
    private ProcessingType type;

    @NotNull
    private MetricUnit quantityMetricUnit;

    @NotNull
    private ProcessName processName;

    @NotEmpty
    @Valid
    private List<ProcessingDistributionDataRequest> data;

    public Set<ProcessingDistribution> toProcessingDistributions(final Forecast forecast) {
        final Set<ProcessingDistribution> processingDistributions = new HashSet<>();
        data.forEach(data -> processingDistributions.add(ProcessingDistribution.builder()
                .processName(processName)
                .quantityMetricUnit(quantityMetricUnit)
                .type(type)
                .date(data.getDate())
                .forecast(forecast)
                .quantity(data.getQuantity())
                .build()));

        return processingDistributions;
    }
}
