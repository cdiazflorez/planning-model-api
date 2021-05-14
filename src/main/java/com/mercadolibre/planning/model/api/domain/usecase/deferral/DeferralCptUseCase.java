package com.mercadolibre.planning.model.api.domain.usecase.deferral;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.gateway.DeferralGateway;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

@Slf4j
@Component
@AllArgsConstructor
public class DeferralCptUseCase implements UseCase<DeferralInput,
        List<CurrentPlanningDistribution>> {

    private final DeferralGateway deferralGateway;

    private final CurrentPlanningDistributionRepository currentPlanningRepository;

    @Override
    public List<CurrentPlanningDistribution> execute(final DeferralInput input) {

        final DeferralDto deferralProjection = deferralGateway.getDeferralProjection(
                input.getLogisticCenterId(),
                input.getWorkflow()
        );

        if (isEmpty(deferralProjection.getProjections())) {
            log.info("No projections found in [{}][{}]",
                    input.getLogisticCenterId(), input.getWorkflow());
            return Collections.emptyList();
        }

        return processDeferral(getPlanning(deferralProjection), deferralProjection);
    }

    private List<CurrentPlanningDistribution> getPlanning(final DeferralDto deferralProjection) {
        final ProjectionDto cptFrom = deferralProjection.getProjections().stream()
                .min(Comparator.comparing(ProjectionDto::getEstimatedTimeDeparture))
                .orElseThrow(NoSuchElementException::new);

        final ProjectionDto cptTo = deferralProjection.getProjections().stream()
                .max(Comparator.comparing(ProjectionDto::getEstimatedTimeDeparture))
                .orElseThrow(NoSuchElementException::new);

        return currentPlanningRepository
                .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                        Workflow.valueOf(deferralProjection.getWorkflow()),
                        deferralProjection.getWarehouseId(),
                        cptFrom.getEstimatedTimeDeparture(),
                        cptTo.getEstimatedTimeDeparture());
    }

    private List<CurrentPlanningDistribution> processDeferral(
            final List<CurrentPlanningDistribution> currentPlanning,
            final DeferralDto deferralProjection) {

        final Map<ZonedDateTime, CurrentPlanningDistribution> planningByCpt = currentPlanning
                .stream().collect(Collectors.toMap(
                        CurrentPlanningDistribution::getDateOut,
                        Function.identity(),
                        (pd1, pd2) -> pd2));

        final List<CurrentPlanningDistribution> processedCurrentPlanningDist =
                deferralProjection.getProjections().stream()
                        .map(cptProjection -> processCpt(
                                Workflow.valueOf(deferralProjection.getWorkflow()),
                                deferralProjection.getWarehouseId(),
                                cptProjection,
                                planningByCpt.get(cptProjection.getEstimatedTimeDeparture())))
                        .filter(Predicate.not(Optional::isEmpty))
                        .map(Optional::get)
                        .collect(Collectors.toList());

        currentPlanningRepository.saveAll(processedCurrentPlanningDist);
        return processedCurrentPlanningDist;
    }

    private Optional<CurrentPlanningDistribution> processCpt(
            final Workflow workflow,
            final String warehouseId,
            final ProjectionDto cptProjection,
            final CurrentPlanningDistribution currentPlanning) {

        if (cptProjection.isShouldDeferral()) {
            if (currentPlanning == null) {
                return Optional.of(CurrentPlanningDistribution
                        .builder()
                        .workflow(workflow)
                        .logisticCenterId(warehouseId)
                        .dateOut(cptProjection.getEstimatedTimeDeparture())
                        .quantity(0)
                        .isActive(true)
                        .build());
            } else {
                currentPlanning.setActive(true);
                return Optional.of(currentPlanning);
            }
        } else if (currentPlanning != null) {
            currentPlanning.setActive(false);
            return Optional.of(currentPlanning);
        }
        return Optional.empty();
    }
}
