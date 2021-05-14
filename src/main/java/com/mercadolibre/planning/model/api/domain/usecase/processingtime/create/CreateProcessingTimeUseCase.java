package com.mercadolibre.planning.model.api.domain.usecase.processingtime.create;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingTimeRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CreateProcessingTimeUseCase implements
        UseCase<CreateProcessingTimeInput, CreateProcessingTimeOutput> {

    private final GetForecastUseCase getForecastUseCase;

    private final CurrentProcessingTimeRepository processingTimeRepository;

    private final PlanningDistributionRepository planningDistributionRepository;

    private final CurrentPlanningDistributionRepository currentPlanningDistributionRep;

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

        final List<Long> forecastIds = getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getLogisticCenterId())
                .dateFrom(input.getCptFrom())
                .dateTo(input.getCptTo())
                .build());

        final List<PlanningDistributionView> planningDistributions = planningDistributionRepository
                .findByWarehouseIdWorkflowAndCptRange(
                        input.getCptFrom(),
                        input.getCptTo(),
                        forecastIds,
                        true);

        final List<CurrentPlanningDistribution> inputCurrentPlanningDist =
                getCurrentPlanningDistributionList(input, planningDistributions);

        currentPlanningDistributionRep.saveAll(inputCurrentPlanningDist);

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

    private List<CurrentPlanningDistribution> getCurrentPlanningDistributionList(
            final CreateProcessingTimeInput input,
            final List<PlanningDistributionView> planningDistributionViews) {

        return planningDistributionViews.stream().map(pd -> CurrentPlanningDistribution
                .builder()
                .workflow(input.getWorkflow())
                .logisticCenterId(input.getLogisticCenterId())
                .dateOut(pd.getDateOut().toInstant()
                        .atZone(ZoneId.systemDefault()))
                .quantity(0)
                .quantityMetricUnit(MetricUnit.UNITS)
                .isActive(true)
                .build())
                .collect(Collectors.toList());
    }
}

