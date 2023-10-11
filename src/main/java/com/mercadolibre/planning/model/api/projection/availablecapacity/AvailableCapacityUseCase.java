package com.mercadolibre.planning.model.api.projection.availablecapacity;

import static java.time.temporal.ChronoUnit.MINUTES;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.SLAProjectionService;
import com.mercadolibre.planning.model.api.projection.builder.PackingProjectionBuilder;
import com.mercadolibre.planning.model.api.projection.builder.Projector;
import com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AvailableCapacityUseCase {

  /**
   * Executes the projection with the parameters received and then with the projection result calculates the throughput
   * between the projected end date and the cutoff of the slas and responds with the minimum.
   * @param executionDateFrom lower range of the time to be calculated
   * @param executionDateTo upper range of the time to be calculated
   * @param currentBacklog backlog that exists in each process opened by process path and sla
   * @param forecastBacklog backlog that
   * @param throughput processing power by operation hour
   * @param cycleTimeBySla cycle time expressed in minutes for each sla
   * @return If all sla could finish in the projection. it returns the lowest tph between all the projected end date and te cut off,
   *     otherwise returns 0.
   */

  public AvailableCapacity execute(
      final Instant executionDateFrom,
      final Instant executionDateTo,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final Map<Instant, Integer> cycleTimeBySla
  ) {
    final Projector projector = new PackingProjectionBuilder();
    final SlaProjectionResult projection = SLAProjectionService.execute(
        executionDateFrom,
        executionDateTo,
        currentBacklog,
        forecastBacklog,
        throughput,
        getCutOffs(cycleTimeBySla),
        projector
    );
    final boolean slasWhereNotFinished = projection.slas().stream()
        .map(SlaProjectionResult.Sla::projectedEndDate)
        .anyMatch(Objects::isNull);

    if (slasWhereNotFinished) {
      return new AvailableCapacity(0, projection);
    }

    final Map<Instant, Integer> minimumTphByHour = ThroughputCalculator.getMinimumTphValueByHour(throughput);

    final var endDateCutOffTupleStream = getProjectedEndDateByCutOffs(projection.slas(), cycleTimeBySla);
    final Integer capacity = endDateCutOffTupleStream
        .map(tuple -> ThroughputCalculator.totalWithinRange(minimumTphByHour, tuple.projectedEndDate, tuple.cutOff))
        .min(Comparator.naturalOrder())
        .orElseThrow();
    return new AvailableCapacity(capacity, projection);
  }

  private static Map<Instant, Instant> getCutOffs(final Map<Instant, Integer> cycleTimeBySla) {
    return cycleTimeBySla.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> calculateCutOffFromSla(entry.getKey(), entry.getValue())));
  }

  private static Stream<EndDateCutOffTuple> getProjectedEndDateByCutOffs(
      final List<SlaProjectionResult.Sla> projectionResult,
      final Map<Instant, Integer> cycleTimeBySla
  ) {
    return projectionResult.stream()
        .map(projection -> new EndDateCutOffTuple(
            projection.projectedEndDate(),
            calculateCutOffFromSla(projection.date(), cycleTimeBySla.getOrDefault(projection.date(), 0))
        ));
  }

  private static Instant calculateCutOffFromSla(final Instant sla, final Integer cycleTime) {
    return sla.minus(cycleTime, MINUTES);
  }

  record EndDateCutOffTuple(Instant projectedEndDate, Instant cutOff) {}
}
