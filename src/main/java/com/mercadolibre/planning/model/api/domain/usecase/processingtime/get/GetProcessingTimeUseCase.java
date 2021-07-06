package com.mercadolibre.planning.model.api.domain.usecase.processingtime.get;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingTimeRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationUseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;

@Service
@AllArgsConstructor
public class GetProcessingTimeUseCase implements
        UseCase<GetProcessingTimeInput, List<GetProcessingTimeOutput>> {

    private final CurrentProcessingTimeRepository repository;
    private final GetConfigurationUseCase getConfigurationUseCase;

    private static final MetricUnit CT_DEFAULT_METRICS = MINUTES;
    private static final long CT_DEFAULT_VALUE = 240;

    @Override
    public List<GetProcessingTimeOutput> execute(final GetProcessingTimeInput input) {

        final List<CurrentProcessingTime> processingTimeDeviations = repository
                .findByWorkflowAndLogisticCenterIdAndIsActiveTrueAndDateBetweenCpt(
                        input.getWorkflow(),
                        input.getLogisticCenterId());

        final Configuration configurationDefault = getConfigurationUseCase.execute(
                new GetConfigurationInput(
                        input.getLogisticCenterId(),
                        "processing_time"))
                .orElseGet(() -> Configuration.builder()
                        .metricUnit(CT_DEFAULT_METRICS)
                        .value(CT_DEFAULT_VALUE)
                        .build());

        final List<GetProcessingTimeOutput> processingTimeOutputs = new ArrayList<>();

        input.getCpt().forEach(cpt -> {

            final long processingTimeValue;
            final MetricUnit processingTimeMetricUnit;

            final List<CurrentProcessingTime> processingTimes = processingTimeDeviations.stream()
                    .filter(item ->
                            (item.getCptFrom().isEqual(cpt) || item.getCptFrom().isBefore(cpt))
                            && (item.getCptTo().isEqual(cpt) || item.getCptTo().isAfter(cpt)))
                    .collect(Collectors.toList());

            processingTimeValue = processingTimes.isEmpty()
                    ? configurationDefault.getValue()
                    : processingTimes.get(0).getValue();

            processingTimeMetricUnit = processingTimes.isEmpty()
                    ? configurationDefault.getMetricUnit()
                    : processingTimes.get(0).getMetricUnit();

            processingTimeOutputs.add(GetProcessingTimeOutput.builder()
                    .cpt(cpt)
                    .logisticCenterId(input.getLogisticCenterId())
                    .workflow(input.getWorkflow())
                    .metricUnit(processingTimeMetricUnit)
                    .value(processingTimeValue)
                    .build());
        });

        return processingTimeOutputs;
    }
}
