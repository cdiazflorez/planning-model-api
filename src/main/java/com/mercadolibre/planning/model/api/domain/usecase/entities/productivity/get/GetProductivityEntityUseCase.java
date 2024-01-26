package com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.plan.staffing.GetStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.newrelic.api.agent.Trace;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 *
 *  @deprecated This use case is deprecated because only one use case will be used to obtain the staffing plan
 *  (Headcount, Productivity, TPH and maximum capacity)., it was replaced by the {@link GetStaffingPlanUseCase}
 *
 */
@Deprecated
@Service
@AllArgsConstructor
public class GetProductivityEntityUseCase implements EntityUseCase<GetProductivityInput, List<ProductivityOutput>> {

  private static final int ABILITY_LEVEL = 1;
  protected final HeadcountProductivityRepository productivityRepository;

  protected final CurrentProcessingDistributionRepository currentProcessingDistributionRepository;

  protected final GetForecastUseCase getForecastUseCase;

  @Trace
  @Override
  public List<ProductivityOutput> execute(final GetProductivityInput input) {
    return Stream.concat(
            getForecastProductivity(input),
            getSimulationProductivity(input)
    ).collect(Collectors.toList());
  }

  @Override
  public boolean supportsEntityType(final EntityType entityType) {
    return entityType == PRODUCTIVITY;
  }

  private Stream<ProductivityOutput> getSimulationProductivity(final GetProductivityInput input) {

    // TODO: revisar si es necesario remover la validación del PP Global y dejar solo la del source.
    if (input.getSource() == FORECAST) {
        return Stream.empty();
    }
    final List<ProductivityOutput> inputSimulatedEntities = createUnappliedSimulations(input);
    final Stream<ProductivityOutput> currentProductivity =  findCurrentProductivityBy(input).stream()
            .filter(noSimulationExistsWithSameProperties(inputSimulatedEntities))
            .map(sp -> ProductivityOutput.builder()
                    .workflow(input.getWorkflow())
                    .value(sp.getQuantity())
                    .source(SIMULATION)
                    .processPath(sp.getProcessPath())
                    .processName(sp.getProcessName())
                    .metricUnit(sp.getQuantityMetricUnit())
                    .date(sp.getDate())
                    .abilityLevel(ABILITY_LEVEL)
                    .build());

    return Stream.concat(
            inputSimulatedEntities.stream(),
            currentProductivity
    );
  }

  private Stream<ProductivityOutput> getForecastProductivity(final GetProductivityInput input) {
    final List<HeadcountProductivityView> productivities = findProductivityBy(input);

    return productivities.stream()
        .map(p -> ProductivityOutput.builder()
            .workflow(input.getWorkflow())
            .date(ofInstant(p.getDate().toInstant(), UTC))
            .processPath(p.getProcessPath())
            .processName(p.getProcessName())
            .value(p.getProductivity())
            .metricUnit(p.getProductivityMetricUnit())
            .source(FORECAST)
            .abilityLevel(p.getAbilityLevel())
            .build());
  }

  private Predicate<CurrentProcessingDistribution> noSimulationExistsWithSameProperties(
          final List<ProductivityOutput> entities
  ) {
     return currentHeadcountProductivity -> entities.stream().noneMatch(entityOutput -> entityOutput.getSource() == SIMULATION
             && entityOutput.getProcessName() == currentHeadcountProductivity.getProcessName()
             && entityOutput.getWorkflow() == currentHeadcountProductivity.getWorkflow()
             && entityOutput.getDate().withFixedOffsetZone()
             .isEqual(currentHeadcountProductivity.getDate().withFixedOffsetZone()));
  }

  private List<CurrentProcessingDistribution> findCurrentProductivityBy(final GetProductivityInput input) {
    final var processPaths = input.getProcessPaths()
        .stream()
        .map(ProcessPath::name)
        .collect(toSet());

    final var processes = new HashSet<>(input.getProcessNamesAsString());

    return currentProcessingDistributionRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
        input.getWarehouseId(),
        input.getWorkflow().name(),
        processPaths,
        processes,
        Set.of(PRODUCTIVITY.name()),
        input.getDateFrom(),
        input.getDateTo(),
        input.viewDate()
    );
  }

  private List<HeadcountProductivityView> findProductivityBy(final GetProductivityInput input) {
    final List<Long> forecastIds = getForecastUseCase.execute(new GetForecastInput(
        input.getWarehouseId(),
        input.getWorkflow(),
        input.getDateFrom(),
        input.getDateTo(),
        input.getViewDate()
    ));

    return productivityRepository.findBy(
        input.getProcessNamesAsString(),
        input.getProcessPathsAsString(),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds,
        input.getAbilityLevel());
  }

  private List<ProductivityOutput> createUnappliedSimulations(final GetProductivityInput input) {
    if (input.getSimulations() == null) {
      return emptyList();
    }

    return input.getSimulations().stream()
      .flatMap(simulation -> simulation.getEntities().stream()
                .filter(entity -> entity.getType() == PRODUCTIVITY)
                .flatMap(entity -> entity.getValues().stream()
                        .map(quantityByDate -> ProductivityOutput.builder()
                                .workflow(input.getWorkflow())
                                .date(quantityByDate.getDate())
                                .metricUnit(MetricUnit.UNITS_PER_HOUR)
                                .processPath(GLOBAL)
                                .processName(simulation.getProcessName())
                                .source(SIMULATION)
                                .value(quantityByDate.getQuantity())
                                .abilityLevel(1)
                                .build())
                    )
            ).collect(Collectors.toList());
  }
}
