package com.mercadolibre.planning.model.api.domain.usecase.simulation;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.web.controller.request.QuantityByDate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class ActivateSimulationUseCase implements UseCase<SimulationInput, List<SimulationOutput>> {

    private static final int ORIGINAL_WORKER_ABILITY = 1;

    private final CurrentHeadcountProductivityRepository currentProductivityRepository;
    private final CurrentProcessingDistributionRepository currentProcessingRepository;

    @Override
    public List<SimulationOutput> execute(final SimulationInput input) {
        deactivateOldSimulations(input);
        return createSimulation(input);
    }

    private void deactivateOldSimulations(final SimulationInput input) {
        final long userId = input.getUserId();

        input.getSimulations().forEach(simulation ->
                simulation.getEntities().stream()
                        .filter(e -> e.getType() == PRODUCTIVITY)
                        .forEach(entity ->
                                currentProductivityRepository.deactivateProductivity(
                                        input.getWarehouseId(),
                                        input.getWorkflow(),
                                        simulation.getProcessName(),
                                        entity.getValues().stream()
                                                .map(QuantityByDate::getDate)
                                                .collect(toList()),
                                        UNITS_PER_HOUR,
                                        userId,
                                        ORIGINAL_WORKER_ABILITY
                                )
                        ));

        input.getSimulations().forEach(simulation ->
                simulation.getEntities().stream()
                        .filter(e -> e.getType() == HEADCOUNT)
                        .forEach(entity ->
                                currentProcessingRepository.deactivateProcessingDistribution(
                                        input.getWarehouseId(),
                                        input.getWorkflow(),
                                        simulation.getProcessName(),
                                        entity.getValues().stream()
                                                .map(QuantityByDate::getDate)
                                                .collect(toList()),
                                        ACTIVE_WORKERS,
                                        userId,
                                        WORKERS
                                )
                        ));
    }

    private List<SimulationOutput> createSimulation(final SimulationInput input) {
        final List<CurrentHeadcountProductivity> simulatedProductivities =
                currentProductivityRepository.saveAll(createProductivities(input));

        final List<CurrentProcessingDistribution> simulatedHeadcount =
                currentProcessingRepository.saveAll(createHeadcount(input));

        return createSimulationOutput(simulatedProductivities, simulatedHeadcount);
    }

    private List<SimulationOutput> createSimulationOutput(
            final List<CurrentHeadcountProductivity> simulatedProductivities,
            final List<CurrentProcessingDistribution> simulatedHeadcount) {

        final List<SimulationOutput> simulationOutputs = new ArrayList<>();
        simulatedHeadcount.forEach(headcount -> simulationOutputs.add(
                new SimulationOutput(
                        headcount.getId(),
                        headcount.getDate(),
                        headcount.getWorkflow(),
                        headcount.getProcessName(),
                        headcount.getQuantity(),
                        headcount.getQuantityMetricUnit(),
                        headcount.isActive(),
                        null
                )
        ));

        simulatedProductivities.forEach(productivity -> simulationOutputs.add(
                new SimulationOutput(
                        productivity.getId(),
                        productivity.getDate(),
                        productivity.getWorkflow(),
                        productivity.getProcessName(),
                        productivity.getProductivity(),
                        productivity.getProductivityMetricUnit(),
                        productivity.isActive(),
                        productivity.getAbilityLevel()
                )
        ));

        return simulationOutputs;
    }

    private List<CurrentHeadcountProductivity> createProductivities(final SimulationInput input) {
        final List<CurrentHeadcountProductivity> simulatedProductivities = new ArrayList<>();

        input.getSimulations().forEach(simulation -> simulation.getEntities().stream()
                .filter(entity -> entity.getType() == PRODUCTIVITY)
                .forEach(entity -> entity.getValues().forEach(value ->
                        simulatedProductivities.add(CurrentHeadcountProductivity.builder()
                                .workflow(input.getWorkflow())
                                .processName(simulation.getProcessName())
                                .date(value.getDate())
                                .logisticCenterId(input.getWarehouseId())
                                .productivity(value.getQuantity())
                                .userId(input.getUserId())
                                .productivityMetricUnit(UNITS_PER_HOUR)
                                .abilityLevel(ORIGINAL_WORKER_ABILITY)
                                .isActive(true)
                                .build()))));

        return simulatedProductivities;
    }

    private List<CurrentProcessingDistribution> createHeadcount(final SimulationInput input) {
        final List<CurrentProcessingDistribution> simulatedHeadcount = new ArrayList<>();

        input.getSimulations().forEach(simulation -> simulation.getEntities().stream()
                .filter(entity -> entity.getType() == HEADCOUNT)
                .forEach(entity -> entity.getValues().forEach(value ->
                        simulatedHeadcount.add(CurrentProcessingDistribution.builder()
                                .workflow(input.getWorkflow())
                                .processName(simulation.getProcessName())
                                .date(value.getDate())
                                .quantity(value.getQuantity())
                                .logisticCenterId(input.getWarehouseId())
                                .quantityMetricUnit(WORKERS)
                                .userId(input.getUserId())
                                .type(ACTIVE_WORKERS)
                                .isActive(true)
                                .build()))));

        return simulatedHeadcount;
    }
}
