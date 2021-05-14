package com.mercadolibre.planning.model.api.domain.usecase.processingtime.create;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingTimeRepository;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CreateProcessingTimeUseCase implements
        UseCase<CreateProcessingTimeInput, CreateProcessingTimeOutput> {

    private final CurrentProcessingTimeRepository processingTimeRepository;

    @Override
    public CreateProcessingTimeOutput execute(final CreateProcessingTimeInput input) {

        final CurrentProcessingTime currentProcessingTime =
                processingTimeRepository.save(CurrentProcessingTime.builder()
                        .value(input.getValue())
                        .logisticCenterId(input.getLogisticCenterId())
                        .metricUnit(input.getMetricUnit())
                        .workflow(input.getWorkflow())
                        .cptFrom(input.getCptFrom())
                        .cptTo(input.getCptTo())
                        .isActive(true)
                        .userId(input.getUserId())
                        .build());

        return CreateProcessingTimeOutput.builder()
                .id(currentProcessingTime.getId())
                .value(currentProcessingTime.getValue())
                .logisticCenterId(currentProcessingTime.getLogisticCenterId())
                .metricUnit(currentProcessingTime.getMetricUnit())
                .cptFrom(currentProcessingTime.getCptFrom())
                .cptTo(currentProcessingTime.getCptTo())
                .dateCreated(currentProcessingTime.getDateCreated())
                .workflow(currentProcessingTime.getWorkflow())
                .lastUpdated(currentProcessingTime.getLastUpdated())
                .userId(currentProcessingTime.getUserId())
                .isActive(currentProcessingTime.isActive())
                .build();
    }
}

