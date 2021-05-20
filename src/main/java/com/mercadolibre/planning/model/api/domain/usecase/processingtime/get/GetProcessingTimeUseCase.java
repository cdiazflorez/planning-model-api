package com.mercadolibre.planning.model.api.domain.usecase.processingtime.get;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingTimeRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationUseCase;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class GetProcessingTimeUseCase implements
        UseCase<GetProcessingTimeInput, GetProcessingTimeOutput> {

    private final CurrentProcessingTimeRepository repository;
    private final GetConfigurationUseCase getConfigurationUseCase;

    @Override
    public GetProcessingTimeOutput execute(final GetProcessingTimeInput input) {
        final long processingTimeValue;
        final MetricUnit processingTimeMetricUnit;
        final List<CurrentProcessingTime> processingTimeDeviations = repository
                .findByWorkflowAndLogisticCenterIdAndIsActiveTrueAndDateBetweenCpt(
                        input.getWorkflow(),
                        input.getLogisticCenterId(),
                        input.getCpt()
                );

        if (processingTimeDeviations.isEmpty()) {
            final Configuration defaultProcessingTime = getConfigurationUseCase.execute(
                    new GetConfigurationInput(input.getLogisticCenterId(), "processing_time"))
                    .orElseThrow(() -> new EntityNotFoundException(
                            "CONFIGURATION",
                            input.getLogisticCenterId() + "processing_time")
                    );

            processingTimeValue = defaultProcessingTime.getValue();
            processingTimeMetricUnit = defaultProcessingTime.getMetricUnit();
        } else {
            final CurrentProcessingTime processingTime = processingTimeDeviations.get(0);
            processingTimeValue = processingTime.getValue();
            processingTimeMetricUnit = processingTime.getMetricUnit();
        }

        return GetProcessingTimeOutput.builder()
                .logisticCenterId(input.getLogisticCenterId())
                .workflow(input.getWorkflow())
                .value(processingTimeValue)
                .metricUnit(processingTimeMetricUnit)
                .build();
    }
}
