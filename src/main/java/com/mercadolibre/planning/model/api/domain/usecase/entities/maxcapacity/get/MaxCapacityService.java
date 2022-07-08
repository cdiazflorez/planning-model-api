package com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.exception.BadSimulationRequestException;
import com.mercadolibre.planning.model.api.exception.InvalidForecastException;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MaxCapacityService {

  private static final int MAXIMUM_NUMBER_OF_LIST_ELEMENTS = 1;

  private final ProcessingDistributionRepository processingDistRepository;

  private final GetForecastUseCase getForecastUseCase;

  public Map<ZonedDateTime, Integer> getMaxCapacity(MaxCapacityInput input) {

    final List<ProcessingDistributionView> maxCapacityBD = processingDistRepository
        .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            Set.of(MAX_CAPACITY.name()),
            List.of(GLOBAL.toJson()),
            input.getDateFrom(),
            input.getDateTo(),
            getForecastIds(input));

    return toMaxCapacityByDate(input, maxCapacityBD);

  }

  private List<Long> getForecastIds(final MaxCapacityInput input) {
    return getForecastUseCase.execute(new GetForecastInput(
        input.getWarehouseId(),
        input.getWorkflow(),
        input.getDateFrom(),
        input.getDateTo()
    ));
  }

  private Map<ZonedDateTime, Integer> toMaxCapacityByDate(final MaxCapacityInput input,
                                                          final List<ProcessingDistributionView> capacities) {
    final Map<Instant, Integer> capacityByDate =
        capacities.stream()
            .collect(
                toMap(
                    o -> o.getDate().toInstant().truncatedTo(SECONDS),
                    o -> (int) o.getQuantity(),
                    (intA, intB) -> intB)
            );

    if (input.getSimulations() != null && !input.getSimulations().isEmpty()) {

      final List<List<QuantityByDate>> globalThroughputSimulations = input.getSimulations().stream()
          .filter(simulation -> simulation.getProcessName().equals(GLOBAL))
          .map(Simulation::getEntities)
          .flatMap(Collection::stream)
          .filter(simulationEntity -> simulationEntity.getType().equals(MAX_CAPACITY))
          .map(SimulationEntity::getValues)
          .collect(Collectors.toList());

      if (globalThroughputSimulations.size() == MAXIMUM_NUMBER_OF_LIST_ELEMENTS) {
        Map<Instant, Integer> simulationDates = globalThroughputSimulations.get(0).stream()
            .collect(toMap(quantityByDate -> quantityByDate.getDate().toInstant(), QuantityByDate::getQuantity));
        capacityByDate.putAll(simulationDates);
      } else {
        throw new BadSimulationRequestException(MAX_CAPACITY.name());
      }

    }

    final int defaultCapacity = capacityByDate.values()
        .stream()
        .max(Integer::compareTo)
        .orElseThrow(() -> new InvalidForecastException(input.getWarehouseId(), input.getWorkflow().name()));

    final Set<Instant> capacityHours = getProcessingCapacityInflectionPointsBetween(input.getDateFrom(), input.getDateTo());

    return capacityHours.stream()
        .collect(
            toMap(
                o -> ZonedDateTime.from(o.atZone(ZoneOffset.UTC)),
                o -> capacityByDate.getOrDefault(o, defaultCapacity),
                (intA, intB) -> intB,
                TreeMap::new)
        );
  }

  private Set<Instant> getProcessingCapacityInflectionPointsBetween(final ZonedDateTime dateFrom, final Temporal dateTo) {
    final Duration intervalBetweenDates = Duration.between(dateFrom, dateTo);
    final ZonedDateTime baseDate = dateFrom.truncatedTo(SECONDS);
    return LongStream.range(0, intervalBetweenDates.toHours())
        .mapToObj(i -> baseDate.plusHours(i).toInstant())
        .collect(toSet());
  }

}
