package com.mercadolibre.planning.model.api.domain.usecase.processingtime.create;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingTimeRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
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
                        .userId(input.getUserId())
                        .build());

        final List<ZonedDateTime> plannedCpts = getPlannedCpts(input);

        final List<CurrentPlanningDistribution> currentPlanningDistributions =
                currentPlanningDistributionRep
                        .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                                input.getWorkflow(),
                                input.getLogisticCenterId(),
                                input.getCptFrom(),
                                input.getCptTo());

        final List<CurrentPlanningDistribution> inputCurrentPlanningDist =
                getCurrentPlanningDistributionList(
                        input,
                        plannedCpts,
                        currentPlanningDistributions);

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
                .build();
    }

    private List<CurrentPlanningDistribution> getCurrentPlanningDistributionList(
            final CreateProcessingTimeInput input,
            final List<ZonedDateTime> plannedCpts,
            final List<CurrentPlanningDistribution> currentPlanningDistributions) {

        final Map<ZonedDateTime, CurrentPlanningDistribution> planningByCpt =
                currentPlanningDistributions.stream()
                        .collect(Collectors.toMap(
                                CurrentPlanningDistribution::getDateOut,
                                Function.identity(),
                                (pd1, pd2) -> pd2));

        return plannedCpts.stream()
                        .map(plannedCpt -> processCpt(
                                input,
                                plannedCpt,
                                planningByCpt.get(plannedCpt)))
                        .filter(Predicate.not(Optional::isEmpty))
                        .map(Optional::get)
                        .collect(Collectors.toList());
    }

    private List<ZonedDateTime> getPlannedCpts(final CreateProcessingTimeInput input) {

        final List<Long> forecastIds = getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getLogisticCenterId())
                .dateFrom(input.getCptFrom())
                .dateTo(input.getCptTo())
                .build());

        return planningDistributionRepository.findByWarehouseIdWorkflowAndCptRange(
                input.getCptFrom(),
                input.getCptTo(),
                forecastIds,
                true)
                .stream().map(item -> item.getDateOut().toInstant().atZone(ZoneId.of("UTC")))
                .collect(Collectors.toList());
    }

    private Optional<CurrentPlanningDistribution> processCpt(
            final CreateProcessingTimeInput input,
            final ZonedDateTime cpt,
            final CurrentPlanningDistribution currentPlanning) {

        if (currentPlanning == null) {
            return Optional.of(CurrentPlanningDistribution
                    .builder()
                    .workflow(input.getWorkflow())
                    .logisticCenterId(input.getLogisticCenterId())
                    .dateOut(cpt)
                    .dateInFrom(cpt.minusMinutes(input.getValue()))
                    .quantity(0)
                    .quantityMetricUnit(MetricUnit.UNITS)
                    .isActive(true)
                    .build());
        } else {
            currentPlanning.setActive(false);
            return Optional.of(currentPlanning);
        }
    }
}

