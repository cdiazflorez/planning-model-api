package com.mercadolibre.planning.model.api.domain.usecase.simulation.activate;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT_SYSTEMIC;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
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
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class ActivateSimulationUseCase implements UseCase<SimulationInput, List<SimulationOutput>> {

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
        simulation.getEntities()
            .forEach(entity ->
                currentProcessingRepository.deactivateProcessingDistribution(
                    input.getWarehouseId(),
                    input.getWorkflow(),
                    simulation.getProcessName(),
                    entity.getValues()
                        .stream()
                        .map(QuantityByDate::getDate)
                        .collect(toList()),
                    entity.getType().getProcessingType(),
                    userId,
                    entity.getType().getMetricUnit()
                )
            )
    );
  }

  private List<SimulationOutput> createSimulation(final SimulationInput input) {
    final List<CurrentProcessingDistribution> currentProcessingDistributions = createProcessingDistribution(input);
    return createSimulationOutput(currentProcessingRepository.saveAll(currentProcessingDistributions));
  }

  private List<SimulationOutput> createSimulationOutput(final List<CurrentProcessingDistribution> simulatedHeadcount) {

    final List<SimulationOutput> simulationOutputs = new ArrayList<>();
    simulatedHeadcount.forEach(processingDistribution -> simulationOutputs.add(
        new SimulationOutput(
            processingDistribution.getId(),
            processingDistribution.getDate(),
            processingDistribution.getWorkflow(),
            processingDistribution.getProcessName(),
            processingDistribution.getQuantity(),
            processingDistribution.getQuantityMetricUnit(),
            processingDistribution.isActive(),
            null
        )
    ));

    return simulationOutputs;
  }

  private List<CurrentProcessingDistribution> createProcessingDistribution(final SimulationInput input) {

    final var currentProcessingDistributionsDefault = input.getSimulations().stream()
        .flatMap(simulation -> simulation.getEntities().stream()
            .flatMap(entity -> entity.getValues().stream()
                .map(value ->
                CurrentProcessingDistribution.builder()
                    .workflow(input.getWorkflow())
                    .processName(simulation.getProcessName())
                    .processPath(GLOBAL)
                    .date(value.getDate())
                    .quantity(value.getQuantity())
                    .logisticCenterId(input.getWarehouseId())
                    .userId(input.getUserId())
                    .type(entity.getType().getProcessingType())
                    .quantityMetricUnit(entity.getType().getMetricUnit())
                    .isActive(true)
                    .build()
            ))
        );

    final var currentProcessingDistributionProcessPath = input.getSimulations().stream()
        .flatMap(simulation -> simulation.getEntities().stream()
            .flatMap(entity -> entity.getValues().stream()
                .filter(value -> value.getProcessPath() != null)
                .flatMap(quantityByDate -> quantityByDate.getProcessPath().entrySet().stream()
                    .map(pp -> CurrentProcessingDistribution.builder()
                        .workflow(input.getWorkflow())
                        .processName(simulation.getProcessName())
                        .processPath(pp.getKey())
                        .quantity(pp.getValue())
                        .date(quantityByDate.getDate())
                        .logisticCenterId(input.getWarehouseId())
                        .userId(input.getUserId())
                        .type(entity.getType().getProcessingType())
                        .quantityMetricUnit(entity.getType().getMetricUnit())
                        .isActive(true)
                        .build()
                    )
                )
            )
        ).collect(toList());

    return Stream.concat(
        currentProcessingDistributionsDefault,
        currentProcessingDistributionProcessPath.isEmpty()
            ? createHeadcountByProcessPath(input)
            : currentProcessingDistributionProcessPath.stream()
    ).collect(toList());
  }

  private Stream<CurrentProcessingDistribution> createHeadcountByProcessPath(final SimulationInput input) {

    final var simulationsByProcessAndDate = input.getSimulations()
            .stream()
            .filter(simulation -> simulation.getEntities()
                .stream()
                .anyMatch(simulationEntity -> simulationEntity.getType() == HEADCOUNT
                || simulationEntity.getType() == HEADCOUNT_SYSTEMIC)
            )
            .collect(
                    groupingBy(
                            Simulation::getProcessName,
                            flatMapping(
                                    simulation -> simulation.getEntities().stream()
                                            .filter(entity -> entity.getType() == HEADCOUNT
                                                || entity.getType() == HEADCOUNT_SYSTEMIC)
                                            .flatMap(entity -> entity.getValues().stream()),
                                    toMap(quantityByDate -> quantityByDate.getDate().toInstant(), QuantityByDate::getQuantity, Integer::sum)
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
                .map(s -> s.get(ratio.processName()))
                .map(s -> s.get(ratio.date()))
                .map(quantity -> CurrentProcessingDistribution.builder()
                        .workflow(input.getWorkflow())
                        .processPath(ratio.processPath)
                        .processName(ratio.processName)
                        .date(ZonedDateTime.ofInstant(ratio.date(), ZoneOffset.UTC))
                        .quantity(quantity * ratio.ratio())
                        .logisticCenterId(input.getWarehouseId())
                        .quantityMetricUnit(WORKERS)
                        .userId(input.getUserId())
                        .type(EFFECTIVE_WORKERS)
                        .isActive(true)
                        .build()
                );
    }

  record RatioAtProcessPathProcessAndDate(
      ProcessPath processPath,
      ProcessName processName,
      Instant date,
      Double ratio) {
  }
}
