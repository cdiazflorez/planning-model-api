package com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GetProductivityEntityUseCase implements EntityUseCase<GetProductivityInput, List<ProductivityOutput>> {

  protected final HeadcountProductivityRepository productivityRepository;

  protected final CurrentHeadcountProductivityRepository currentProductivityRepository;

  protected final GetForecastUseCase getForecastUseCase;

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

    if (input.getSource() == FORECAST || !input.getProcessPaths().contains(GLOBAL)) {
        return Stream.empty();
    }
    final List<ProductivityOutput> inputSimulatedEntities = createUnappliedSimulations(input);
    final Stream<ProductivityOutput> currentProductivity =  findCurrentProductivityBy(input).stream()
            .filter(noSimulationExistsWithSameProperties(inputSimulatedEntities))
            .map(sp -> ProductivityOutput.builder()
                    .workflow(input.getWorkflow())
                    .value(sp.getProductivity())
                    .source(SIMULATION)
                    .processPath(GLOBAL)
                    .processName(sp.getProcessName())
                    .metricUnit(sp.getProductivityMetricUnit())
                    .date(sp.getDate())
                    .abilityLevel(sp.getAbilityLevel())
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

  private Predicate<CurrentHeadcountProductivity> noSimulationExistsWithSameProperties(
          final List<ProductivityOutput> entities
  ) {
     return currentHeadcountProductivity -> entities.stream().noneMatch(entityOutput -> entityOutput.getSource() == SIMULATION
             && entityOutput.getProcessName() == currentHeadcountProductivity.getProcessName()
             && entityOutput.getWorkflow() == currentHeadcountProductivity.getWorkflow()
             && entityOutput.getDate().withFixedOffsetZone()
             .isEqual(currentHeadcountProductivity.getDate().withFixedOffsetZone()));
  }

  private List<CurrentHeadcountProductivity> findCurrentProductivityBy(final GetProductivityInput input) {
    final var processes = input.getProcessName()
        .stream()
        .map(ProcessName::name)
        .collect(toSet());

    return currentProductivityRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
        input.getWarehouseId(),
        input.getWorkflow().name(),
        processes,
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
