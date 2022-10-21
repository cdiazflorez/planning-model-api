package com.mercadolibre.planning.model.api.domain.usecase.simulation.activate;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static java.util.stream.Collectors.*;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.service.headcount.ProcessPathHeadcountShareService;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ActivateSimulationUseCase implements UseCase<SimulationInput, List<SimulationOutput>> {

  private static final int ORIGINAL_WORKER_ABILITY = 1;

  private final CurrentHeadcountProductivityRepository currentProductivityRepository;

  private final CurrentProcessingDistributionRepository currentProcessingRepository;

  private final ProcessPathHeadcountShareService processPathHeadcountShareService;

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
            .filter(e -> e.getType() == HEADCOUNT || e.getType() == MAX_CAPACITY)
            .forEach(entity ->
                currentProcessingRepository.deactivateProcessingDistribution(
                    input.getWarehouseId(),
                    input.getWorkflow(),
                    simulation.getProcessName(),
                    entity.getValues().stream()
                        .map(QuantityByDate::getDate)
                        .collect(toList()),
                    entity.getType() == HEADCOUNT ? ACTIVE_WORKERS : ProcessingType.MAX_CAPACITY,
                    userId,
                    entity.getType() == HEADCOUNT ? WORKERS : UNITS_PER_HOUR
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

    final var currentProcessingDistributionsDefault = input.getSimulations().stream()
            .flatMap(simulation -> simulation.getEntities().stream()
                    .filter(entity -> entity.getType() == HEADCOUNT || entity.getType() == MAX_CAPACITY)
                    .flatMap(entity -> entity.getValues().stream().map(value ->
                            CurrentProcessingDistribution.builder()
                                    .workflow(input.getWorkflow())
                                    .processName(simulation.getProcessName())
                                    .processPath(GLOBAL)
                                    .date(value.getDate())
                                    .quantity(value.getQuantity())
                                    .logisticCenterId(input.getWarehouseId())
                                    .quantityMetricUnit(entity.getType() == HEADCOUNT ? WORKERS : UNITS_PER_HOUR)
                                    .userId(input.getUserId())
                                    .type(entity.getType() == HEADCOUNT ? ACTIVE_WORKERS : ProcessingType.MAX_CAPACITY)
                                    .isActive(true)
                                    .build()
                    ))
            );

    return Stream.concat(
            currentProcessingDistributionsDefault,
            createHeadcountByProcessPath(input)
            ).collect(toList());

  }

  private Stream<CurrentProcessingDistribution> createHeadcountByProcessPath(final SimulationInput input) {

    final Set<ProcessName> processNames = input.getSimulations().stream()
            .map(Simulation::getProcessName)
            .collect(Collectors.toSet());

    final var simulationEntities = input.getSimulations().stream()
            .filter(simulation -> processNames.stream().anyMatch(processName -> simulation.getProcessName() == processName))
            .flatMap(simulation -> simulation.getEntities().stream()
                    .filter(simulationEntity -> simulationEntity.getType() == HEADCOUNT)
            ).collect(toList());

    if (simulationEntities.size() > 0) {

      final var dateFrom = simulationEntities.stream()
                      .map(simulationEntity -> simulationEntity.getValues().stream()
                              .min(Comparator.comparing(QuantityByDate::getDate))
                              .map(QuantityByDate::getDate)
                              .get()
                      ).min(ZonedDateTime::compareTo).get();

      final var dateTo = simulationEntities.stream()
                      .map(simulationEntity -> simulationEntity.getValues().stream()
                              .max(Comparator.comparing(QuantityByDate::getDate))
                              .map(QuantityByDate::getDate)
                              .get()
                      ).max(ZonedDateTime::compareTo).get();

      final var dateByProcessNameAndByProcessPath = processPathHeadcountShareService.getHeadcountShareByProcessPath(
              input.getWarehouseId(),
              input.getWorkflow(),
              processNames,
              dateFrom.toInstant(),
              dateTo.toInstant(),
              Instant.now()
      );

      if (dateByProcessNameAndByProcessPath.keySet().size() > 1) {

        dateByProcessNameAndByProcessPath.remove(GLOBAL);

        return input.getSimulations().stream()
                .flatMap(simulation -> simulation.getEntities().stream()
                       .filter(simulationEntity -> simulationEntity.getType() == HEADCOUNT)
                       .map(SimulationEntity::getValues)
                       .flatMap(quantityByDates -> quantityByDates.stream()
                               .flatMap(quantityByDate -> dateByProcessNameAndByProcessPath.entrySet().stream()
                                       .flatMap(processPathMapEntry -> processPathMapEntry.getValue().entrySet().stream()
                                               .filter(processNameMapEntry -> processNameMapEntry.getKey() == simulation.getProcessName())
                                               .flatMap(processNameMapEntry -> processNameMapEntry.getValue().entrySet().stream()
                                                       .filter(ratioByDate -> ratioByDate.getKey().equals(quantityByDate.getDate().toInstant()))
                                                       .map(ratioByDate -> CurrentProcessingDistribution.builder()
                                                               .workflow(input.getWorkflow())
                                                               .processName(simulation.getProcessName())
                                                               .processPath(processPathMapEntry.getKey())
                                                               .date(ZonedDateTime.ofInstant(ratioByDate.getKey(), ZoneId.of("UTC")))
                                                               .quantity(Math.round(quantityByDate.getQuantity() * ratioByDate.getValue()))
                                                               .logisticCenterId(input.getWarehouseId())
                                                               .quantityMetricUnit(WORKERS)
                                                               .userId(input.getUserId())
                                                               .type(ACTIVE_WORKERS)
                                                               .isActive(true)
                                                               .build())
                                               )
                                       ))
                       )
                );

      }

    }

    return Stream.empty();
  }
}
