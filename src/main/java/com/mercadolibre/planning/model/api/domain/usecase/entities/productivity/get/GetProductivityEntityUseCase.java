package com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get;

import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.toList;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    if (input.getSource() == FORECAST) {
      return getForecastProductivity(input);
    } else {
      return getSimulationAndForecastProductivity(input);
    }
  }

  @Override
  public boolean supportsEntityType(final EntityType entityType) {
    return entityType == PRODUCTIVITY;
  }
  
  private List<ProductivityOutput> getSimulationAndForecastProductivity(final GetProductivityInput input) {
    final List<ProductivityOutput> productivityAccumulator = getForecastProductivity(input);
    final List<ProductivityOutput> inputSimulatedEntities = createUnappliedSimulations(input);

    findCurrentProductivityBy(input).forEach(sp -> {
      if (noSimulationExistsWithSameProperties(inputSimulatedEntities, sp)) {
        productivityAccumulator.add(ProductivityOutput.builder()
            .workflow(input.getWorkflow())
            .value(sp.getProductivity())
            .source(SIMULATION)
            .processName(sp.getProcessName())
            .metricUnit(sp.getProductivityMetricUnit())
            .date(sp.getDate())
            .abilityLevel(sp.getAbilityLevel())
            .build());
      }
    });

    productivityAccumulator.addAll(inputSimulatedEntities);
    return new ArrayList<>(productivityAccumulator);
  }
  
  private List<ProductivityOutput> getForecastProductivity(final GetProductivityInput input) {
    final List<HeadcountProductivityView> productivities = findProductivityBy(input);

    return productivities.stream()
        .map(p -> ProductivityOutput.builder()
            .workflow(input.getWorkflow())
            .date(ofInstant(p.getDate().toInstant(), UTC))
            .processName(p.getProcessName())
            .value(p.getProductivity())
            .metricUnit(p.getProductivityMetricUnit())
            .source(FORECAST)
            .abilityLevel(p.getAbilityLevel())
            .build())
        .collect(toList());
  }
  
  private boolean noSimulationExistsWithSameProperties(
      final List<ProductivityOutput> entities,
      final CurrentHeadcountProductivity currentHeadcountProductivity
  ) {

    return entities.stream().noneMatch(entityOutput -> entityOutput.getSource() == SIMULATION
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
        input.getDateTo()
    ));

    return productivityRepository.findBy(
        input.getProcessNamesAsString(),
        input.getDateFrom(),
        input.getDateTo(),
        forecastIds,
        input.getAbilityLevel());
  }

  private List<ProductivityOutput> createUnappliedSimulations(final GetProductivityInput input) {
    if (input.getSimulations() == null) {
      return Collections.emptyList();
    }
    final List<ProductivityOutput> simulatedEntities = new ArrayList<>();

    input.getSimulations().forEach(simulation ->
        simulation.getEntities().stream()
            .filter(entity -> entity.getType() == PRODUCTIVITY)
            .forEach(entity -> entity.getValues().forEach(quantityByDate ->
                simulatedEntities.add(ProductivityOutput.builder()
                    .workflow(input.getWorkflow())
                    .date(quantityByDate.getDate().withFixedOffsetZone())
                    .metricUnit(MetricUnit.UNITS_PER_HOUR)
                    .processName(simulation.getProcessName())
                    .source(SIMULATION)
                    .value(quantityByDate.getQuantity())
                    .abilityLevel(1)
                    .build()))));
    return simulatedEntities;
  }
}
