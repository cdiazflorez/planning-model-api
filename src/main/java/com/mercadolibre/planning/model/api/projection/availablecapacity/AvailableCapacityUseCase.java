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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AvailableCapacityUseCase {

  private static Map<Instant, Instant> getCutOffs(final Map<Instant, Integer> cycleTimeBySla) {
    return cycleTimeBySla.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> calculateCutOffFromSla(entry.getKey(), entry.getValue())));
  }

  private static List<EndDateCutOffBySLA> getProjectedEndDateByCutOffsAndSLA(
      final List<SlaProjectionResult.Sla> projectionResult,
      final Map<Instant, Integer> cycleTimeBySla
  ) {
    return projectionResult.stream()
        .map(projection -> new EndDateCutOffBySLA(
            projection.date(),
            projection.projectedEndDate(),
            calculateCutOffFromSla(projection.date(), cycleTimeBySla.getOrDefault(projection.date(), 0))
        ))
        .toList();
  }

  private static Instant calculateCutOffFromSla(final Instant sla, final Integer cycleTime) {
    return sla.minus(cycleTime, MINUTES);
  }

  /**
   * A sla that does not have capacity is a sla whose projected end date is null or is after it's cut off.
   *
   * @param endDateCutOffBySLA a stream of tuples that must be obtained the slas that do not have capacity.
   * @return a list of {@link Instant} that represent those sla's with zero capacity.
   */
  private static List<Instant> getSlasWithoutCapacity(final List<EndDateCutOffBySLA> endDateCutOffBySLA) {
    return endDateCutOffBySLA.stream()
        .filter(record -> record.projectedEndDate() == null || record.projectedEndDate().isAfter(record.cutOff()))
        .map(EndDateCutOffBySLA::date)
        .toList();
  }

  private static List<CapacityBySLA> calculateMinCapacityBySLA(final List<CapacityBySLA> capacityBySLA) {
    return capacityBySLA.stream()
        .map(currentHour -> new CapacityBySLA(currentHour.date(), getMinCapBetweenSlaAndFollowingOnes(capacityBySLA, currentHour)))
        .toList();
  }

  /**
   * Obtains the minimum capacity between the Sla received and the following Sla.
   *
   * @param capacityBySLA SLA list with its available capacity.
   * @param sinceSLA      SLA in which the available slow shipment capacity is calculated.
   * @return a {@link CapacityBySLA} that has the SLA and its available capacity taking into account the following SLAs
   */
  private static int getMinCapBetweenSlaAndFollowingOnes(final List<CapacityBySLA> capacityBySLA, final CapacityBySLA sinceSLA) {
    return capacityBySLA.stream()
        .filter(sla -> !sla.date().isBefore(sinceSLA.date()))
        .mapToInt(CapacityBySLA::capacity)
        .min()
        .orElse(sinceSLA.capacity());
  }

  /**
   * Executes the projection with the parameters received and then with the projection result calculates the throughput
   * between the projected end date and the cutoff of the slas and responds with the minimum capacity by each SLA.s
   *
   * @param executionDateFrom lower range of the time to be calculated
   * @param executionDateTo   upper range of the time to be calculated
   * @param currentBacklog    backlog that exists in each process opened by process path and sla
   * @param forecastBacklog   backlog that
   * @param throughput        processing power by operation hour
   * @param cycleTimeBySla    cycle time expressed in minutes for each sla
   * @return the minimum capacity calculated by each SLA.
   */
  public List<CapacityBySLA> execute(
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

    final var endDateCutOffBySLA = getProjectedEndDateByCutOffsAndSLA(projection.slas(), cycleTimeBySla);

    final var slasWithoutCapacity = getSlasWithoutCapacity(endDateCutOffBySLA);
    final var capacityBySLAWithoutCapacity = slasWithoutCapacity.stream()
        .map(each -> new CapacityBySLA(each, 0));

    final var capacityBySLAWithCapacity = endDateCutOffBySLA.stream()
        .filter(each -> !slasWithoutCapacity.contains(each.date));

    final Map<Instant, Integer> minimumTphByHour = ThroughputCalculator.getMinimumTphValueByHour(throughput);
    final var capacityBySLAS = capacityBySLAWithCapacity
        .map(tuple -> new CapacityBySLA(tuple.date,
            ThroughputCalculator.totalWithinRange(minimumTphByHour, tuple.projectedEndDate, tuple.cutOff)));
    final var capacityBySLA = Stream.concat(capacityBySLAS, capacityBySLAWithoutCapacity)
        .sorted(Comparator.comparing(CapacityBySLA::date))
        .toList();

    return calculateMinCapacityBySLA(capacityBySLA);
  }

  record EndDateCutOffBySLA(Instant date, Instant projectedEndDate, Instant cutOff) {
  }
}
