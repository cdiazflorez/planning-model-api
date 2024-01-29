package com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.util.DateUtils.fromDate;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.plan.staffing.GetStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.newrelic.api.agent.Trace;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

  /**
   * @deprecated This use case is deprecated because only one use case will be used to obtain the staffing plan (Headcount,
   *  Productivity, TPH and maximum capacity)., it was replaced by the {@link GetStaffingPlanUseCase}.
   */
@Deprecated
@Service
@AllArgsConstructor
public class GetHeadcountEntityUseCase implements EntityUseCase<GetHeadcountInput, List<EntityOutput>> {

  private final ProcessingDistributionRepository processingDistRepository;

  private final CurrentProcessingDistributionRepository currentPDistributionRepository;

  private final GetForecastUseCase getForecastUseCase;

  @Override
  public boolean supportsEntityType(final EntityType entityType) {
    return entityType == HEADCOUNT;
  }

  @Trace
  @Override
  public List<EntityOutput> execute(final GetHeadcountInput input) {
    return Stream.concat(
        getForecastHeadcount(input),
        getSimulationHeadcount(input)
    ).collect(toList());
  }

  private Stream<EntityOutput> getForecastHeadcount(final GetHeadcountInput input) {
    final List<ProcessingDistributionView> processingDistributions = findProcessingDistributionBy(input);

    return processingDistributions.stream()
        .map(p -> EntityOutput.builder()
            .workflow(input.getWorkflow())
            .date(fromDate(p.getDate()))
            .processPath(p.getProcessPath())
            .processName(p.getProcessName())
            .value(p.getQuantity())
            .metricUnit(p.getQuantityMetricUnit())
            .type(p.getType())
            .source(FORECAST)
            .build());
  }

  private List<ProcessingDistributionView> findProcessingDistributionBy(final GetHeadcountInput input) {
    // TODO: move this to an adapter
    final List<Long> forecastIds = getForecastUseCase.execute(new GetForecastInput(
        input.getWarehouseId(),
        input.getWorkflow(),
        input.getDateFrom(),
        input.getDateTo(),
        input.getViewDate())
    );

    final var processPathsNames = input.getProcessPaths()
        .stream()
        .map(ProcessPath::name)
        .toList();

    return processingDistRepository.findByTypeProcessPathProcessNameAndDateInRange(
        getProcessingTypeAsStringOrNull(input.getProcessingType()),
        processPathsNames,
        input.getProcessNamesAsString(),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds
    );
  }

  private Stream<EntityOutput> getSimulationHeadcount(final GetHeadcountInput input) {
    if (input.getSource() == FORECAST) {
      return Stream.empty();
    }

    final List<EntityOutput> inputSimulatedEntities = createUnappliedSimulations(input);

    final Set<EntityOutputKey> unappliedSimulationKeys = inputSimulatedEntities.stream()
        .map(EntityOutputKey::from)
        .collect(toSet());

    final var storedSimulations = findCurrentProcessingDistributionBy(input)
        .stream()
        .filter(simulation -> !unappliedSimulationKeys.contains(EntityOutputKey.from(simulation)))
        .map(cpd -> EntityOutput.builder()
            .workflow(input.getWorkflow())
            .date(cpd.getDate())
            .value(cpd.getQuantity())
            .source(SIMULATION)
            .processPath(cpd.getProcessPath())
            .processName(cpd.getProcessName())
            .metricUnit(cpd.getQuantityMetricUnit())
            .type(cpd.getType())
            .build()
        );

    return Stream.concat(inputSimulatedEntities.stream(), storedSimulations);
  }

  private List<CurrentProcessingDistribution> findCurrentProcessingDistributionBy(final GetHeadcountInput input) {
    // TODO: move this to an adapter
    final var types = Optional.ofNullable(input.getProcessingType())
        .map(processingTypes -> processingTypes.stream()
            .map(ProcessingType::name)
            .collect(toSet()))
        .orElse(Set.of(EFFECTIVE_WORKERS.name()));

    final var processes = new HashSet<>(input.getProcessNamesAsString());

    final var processPathsNames = input.getProcessPaths()
        .stream()
        .map(ProcessPath::name)
        .collect(toSet());

    return currentPDistributionRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
        input.getWarehouseId(),
        input.getWorkflow().name(),
        processPathsNames,
        processes,
        types,
        input.getDateFrom(),
        input.getDateTo(),
        input.viewDate()
    );
  }

  private Set<String> getProcessingTypeAsStringOrNull(final Set<ProcessingType> processingTypes) {
    return processingTypes == null
        ? null
        : processingTypes.stream().map(Enum::name).collect(toSet());
  }

  private List<EntityOutput> createUnappliedSimulations(final GetHeadcountInput input) {
    if (input.getSimulations() == null) {
      return Collections.emptyList();
    }

    return input.getSimulations()
        .stream()
        .flatMap(simulation -> simulation.getEntities().stream()
            .filter(entity -> entity.getType() == HEADCOUNT || entity.getType() == MAX_CAPACITY)
            .flatMap(entity -> entity.getValues()
                .stream()
                .map(quantityByDate -> new EntityOutput(
                    input.getWorkflow(),
                    quantityByDate.getDate().withFixedOffsetZone(),
                    ProcessPath.GLOBAL,
                    simulation.getProcessName(),
                    entity.getType() == HEADCOUNT ? EFFECTIVE_WORKERS : ProcessingType.MAX_CAPACITY,
                    entity.getType().getMetricUnit(),
                    SIMULATION,
                    quantityByDate.getQuantity(),
                    quantityByDate.getQuantity()))))
        .collect(toList());
  }

  @Value
  static class EntityOutputKey {
    Workflow workflow;

    ProcessPath processPath;

    ProcessName processName;

    ProcessingType type;

    ZonedDateTime date;

    static EntityOutputKey from(EntityOutput entity) {
      return new EntityOutputKey(
          entity.getWorkflow(),
          entity.getProcessPath(),
          entity.getProcessName(),
          entity.getType(),
          entity.getDate()
      );
    }

    static EntityOutputKey from(CurrentProcessingDistribution entity) {
      return new EntityOutputKey(
          entity.getWorkflow(),
          entity.getProcessPath(),
          entity.getProcessName(),
          entity.getType(),
          entity.getDate()
      );
    }
  }
}
