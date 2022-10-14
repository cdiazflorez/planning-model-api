package com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
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
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

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

  @Override
  public List<EntityOutput> execute(final GetHeadcountInput input) {
    if (input.getSource() == FORECAST) {
      return getForecastHeadcount(input);
    } else {
      return getSimulationHeadcount(input);
    }
  }

  private List<EntityOutput> getForecastHeadcount(final GetHeadcountInput input) {
    final List<ProcessingDistributionView> processingDistributions =
        findProcessingDistributionBy(input);

    return processingDistributions.stream()
        .map(p -> EntityOutput.builder()
            .workflow(input.getWorkflow())
            .date(fromDate(p.getDate()))
            .processPath(ProcessPath.GLOBAL)
            .processName(p.getProcessName())
            .quantity(p.getQuantity())
            .metricUnit(p.getQuantityMetricUnit())
            .type(p.getType())
            .source(FORECAST)
            .build())
        .collect(toList());
  }

  private List<EntityOutput> getSimulationHeadcount(final GetHeadcountInput input) {
    final List<CurrentProcessingDistribution> currentProcessingDistributions =
        findCurrentProcessingDistributionBy(input);

    final List<EntityOutput> entities = getForecastHeadcount(input);
    final List<EntityOutput> inputSimulatedEntities = createUnappliedSimulations(input);

    currentProcessingDistributions.forEach(cpd -> {
      if (noSimulationExistsWithSameProperties(inputSimulatedEntities, cpd)) {
        entities.add(
            EntityOutput.builder()
                .workflow(input.getWorkflow())
                .date(cpd.getDate())
                .quantity(cpd.getQuantity())
                .source(SIMULATION)
                .processPath(ProcessPath.GLOBAL)
                .processName(cpd.getProcessName())
                .metricUnit(cpd.getQuantityMetricUnit())
                .type(cpd.getType())
                .build());

      }
    });

    entities.addAll(inputSimulatedEntities);
    return new ArrayList<>(entities);
  }

  private boolean noSimulationExistsWithSameProperties(final List<EntityOutput> entities,
                                                       final CurrentProcessingDistribution cpd) {
    return entities.stream().noneMatch(entityOutput -> entityOutput.getSource() == SIMULATION
        && entityOutput.getProcessName() == cpd.getProcessName()
        && entityOutput.getWorkflow() == cpd.getWorkflow()
        && entityOutput.getDate().withFixedOffsetZone()
        .isEqual(cpd.getDate().withFixedOffsetZone()));
  }

  private List<ProcessingDistributionView> findProcessingDistributionBy(final GetHeadcountInput input) {
    final List<Long> forecastIds = getForecastUseCase.execute(new GetForecastInput(
        input.getWarehouseId(),
        input.getWorkflow(),
        input.getDateFrom(),
        input.getDateTo(),
        input.getViewDate()
    ));

    return processingDistRepository
        .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            getProcessingTypeAsStringOrNull(input.getProcessingType()),
            input.getProcessNamesAsString(),
            input.getDateFrom(),
            input.getDateTo(),
            forecastIds);
  }

  private List<CurrentProcessingDistribution> findCurrentProcessingDistributionBy(final GetHeadcountInput input) {
    final var types = Optional.ofNullable(input.getProcessingType())
        .map(processingTypes -> processingTypes.stream()
            .map(ProcessingType::name)
            .collect(toSet())
        ).orElse(Set.of(ProcessingType.ACTIVE_WORKERS.name()));

    final var processes = input.getProcessName()
        .stream()
        .map(ProcessName::name)
        .collect(toSet());

    return currentPDistributionRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
        input.getWarehouseId(),
        input.getWorkflow().name(),
        processes,
        types,
        input.getDateFrom(),
        input.getDateTo(),
        input.viewDate()
    );
  }

  private Set<String> getProcessingTypeAsStringOrNull(
      final Set<ProcessingType> processingTypes) {
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
        .flatMap(simulation ->
            simulation.getEntities().stream()
                .filter(entity -> entity.getType() == HEADCOUNT || entity.getType() == MAX_CAPACITY)
                .flatMap(entity -> entity.getValues()
                    .stream()
                    .map(quantityByDate -> new EntityOutput(
                            input.getWorkflow(),
                            quantityByDate.getDate().withFixedOffsetZone(),
                            ProcessPath.GLOBAL,
                            simulation.getProcessName(),
                            entity.getType() == HEADCOUNT ? ACTIVE_WORKERS : ProcessingType.MAX_CAPACITY,
                            entity.getType() == HEADCOUNT ? WORKERS : UNITS_PER_HOUR,
                            SIMULATION,
                            quantityByDate.getQuantity()
                        )
                    )
                )
        ).collect(toList());
  }

}
