package com.mercadolibre.planning.model.api.domain.usecase.capacity;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Value
@Builder
public class CapacityInput {
    private ZonedDateTime date;
    private ProcessName processName;
    private ProcessingType type;
    private MetricUnit metricUnit;
    private Source source;
    private long value;

    public static List<CapacityInput> fromEntityOutputs(final List<EntityOutput> entityOutputs) {
        return entityOutputs.stream()
                .map(entityOutput ->
                        CapacityInput.builder()
                                .date(entityOutput.getDate())
                                .processName(entityOutput.getProcessName())
                                .type(entityOutput.getType())
                                .metricUnit(entityOutput.getMetricUnit())
                                .source(entityOutput.getSource())
                                .value(entityOutput.getRoundedValue())
                                .build()
                )
                .collect(toList());
    }
}
