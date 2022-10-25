package com.mercadolibre.planning.model.api.domain.usecase.simulation.activate;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.service.headcount.ProcessPathHeadcountShareService;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Value;
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

    final var simulationsByProcessAndDate = input.getSimulations()
            .stream()
            .filter(simulation -> simulation.getEntities().stream().anyMatch(simulationEntity -> simulationEntity.getType() == HEADCOUNT))
            .collect(
                    groupingBy(
                            Simulation::getProcessName,
                            flatMapping(
                                    simulation -> simulation.getEntities().stream().flatMap(entity -> entity.getValues().stream()),
                                    toMap(quantityByDate -> quantityByDate.getDate().toInstant(), QuantityByDate::getQuantity)
                            )
                    )
            );

    if (!simulationsByProcessAndDate.isEmpty()) {

      final List<RatioAtProcessPathProcessAndDate> ratioAtProcessPathProcessAndDates = getRatioByDateProcessAndProcessPath(
              input.getWarehouseId(),
              input.getWorkflow(),
              simulationsByProcessAndDate
      );

      if (!ratioAtProcessPathProcessAndDates.isEmpty()) {

        return ratioAtProcessPathProcessAndDates.stream()
                .map(ratio -> applyRatio(input, ratio, simulationsByProcessAndDate))
                .filter(Optional::isPresent)
                .map(Optional::get);

      }

    }

    return Stream.empty();
  }

  private List<RatioAtProcessPathProcessAndDate> getRatioByDateProcessAndProcessPath(
          final String logisticCenterId,
          final Workflow workflow,
          final Map<ProcessName, Map<Instant, Integer>> simulationEntities
  ) {

      final var dateFrom = simulationEntities.values().stream()
              .flatMap(quantityByDate -> quantityByDate.keySet().stream())
              .min(Instant::compareTo)
              .orElseThrow();

      final var dateTo = simulationEntities.values().stream()
              .flatMap(quantityByDate -> quantityByDate.keySet().stream())
              .max(Instant::compareTo)
              .orElseThrow();

      return processPathHeadcountShareService.getHeadcountShareByProcessPath(
              logisticCenterId,
              workflow,
              simulationEntities.keySet(),
              dateFrom,
              dateTo,
              Instant.now()
              )
              .entrySet()
              .stream()
              .filter(processPathMapEntry -> processPathMapEntry.getKey() != GLOBAL)
              .flatMap(processPathMapEntry -> processPathMapEntry.getValue().entrySet().stream()
                      .flatMap(processNameMapEntry -> processNameMapEntry.getValue().entrySet().stream()
                              .map(ratioByDate -> new RatioAtProcessPathProcessAndDate(processPathMapEntry.getKey(),
                                      processNameMapEntry.getKey(),
                                      ratioByDate.getKey(),
                                      ratioByDate.getValue()))
                      )
              ).collect(toList());
  }

    private Optional<CurrentProcessingDistribution> applyRatio(
            final SimulationInput input,
            final RatioAtProcessPathProcessAndDate ratio,
            final Map<ProcessName, Map<Instant, Integer>> simulationEntities
    ) {
        return Optional.ofNullable(simulationEntities)
                .map(s -> s.get(ratio.getProcessName()))
                .map(s -> s.get(ratio.getDate()))
                .map(quantity -> CurrentProcessingDistribution.builder()
                        .workflow(input.getWorkflow())
                        .processPath(ratio.processPath)
                        .processName(ratio.processName)
                        .date(ZonedDateTime.ofInstant(ratio.getDate(), ZoneOffset.UTC))
                        .quantity(quantity * ratio.getRatio())
                        .logisticCenterId(input.getWarehouseId())
                        .quantityMetricUnit(WORKERS)
                        .userId(input.getUserId())
                        .type(ACTIVE_WORKERS)
                        .isActive(true)
                        .build()
                );
    }

  @Value
  static class RatioAtProcessPathProcessAndDate {

      ProcessPath processPath;

      ProcessName processName;

      Instant date;

      Double ratio;

  }
}
