package com.mercadolibre.planning.model.api.domain.usecase.ratios;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.api.client.db.repository.ratios.RatiosRepository;
import com.mercadolibre.planning.model.api.domain.entity.ratios.Ratio;
import com.mercadolibre.planning.model.api.web.controller.ratios.response.PackingRatio;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class GetPackingWallRatiosService {

  private static final String PW_RATIOS_TYPE = "packing_wall_ratios";

  private static final int DEFAULT_WEEKS = 3;

  private static final int DAYS_IN_A_WEEK = 7;

  private static final int GROUPING_KEY_DAYS_MULTIPLIER = 100;

  private static final double DEFAULT_RATIO = 0.5;

  private final RatiosRepository ratiosRepository;

  private static double calculateFinalRatio(final double partialRatio, final double totalAssignedRatio) {
    return totalAssignedRatio == 0
        ? DEFAULT_RATIO
        : partialRatio + ((partialRatio / totalAssignedRatio) * (1 - totalAssignedRatio));
  }

  private static int groupingKey(final Ratio ratios) {
    final ZonedDateTime zonedDate = ZonedDateTime.ofInstant(ratios.getDate(), ZoneOffset.UTC);
    return groupingKey(zonedDate);
  }

  private static int groupingKey(final ZonedDateTime zonedDate) {
    return zonedDate.getDayOfWeek().getValue() * GROUPING_KEY_DAYS_MULTIPLIER + zonedDate.getHour();
  }

  private static PackingRatio calculateRatiosByDate(final RatioInputGroup input, final Instant dateFrom) {
    final double packingWallPartialRatio = summarizeRatiosApplyWeights(input.getPackingWallUnits(), dateFrom);
    final double packingTotePartialRatio = summarizeRatiosApplyWeights(input.getPackingToteUnits(), dateFrom);

    final double assignedRatio = packingTotePartialRatio + packingWallPartialRatio;

    final double packingToteFinalRatio = calculateFinalRatio(packingTotePartialRatio, assignedRatio);
    final double packingWallFinalRatio = calculateFinalRatio(packingWallPartialRatio, assignedRatio);

    return new PackingRatio(packingToteFinalRatio, packingWallFinalRatio);
  }

  private static double summarizeRatiosApplyWeights(final List<UnitsPerDate> units, final Instant dateFrom) {
    return units.stream().mapToDouble(fh -> calculateWeekWeight(fh.date, dateFrom) * fh.quantity).sum();
  }

  private static Double calculateWeekWeight(final Instant date, final Instant dateFrom) {
    final long week = (DAYS.between(date.truncatedTo(DAYS), dateFrom) - 1) / 7;
    if (week >= DEFAULT_WEEKS) {
      return 0D;
    } else {
      return (double) (DEFAULT_WEEKS - week);
    }
  }

  public Map<Instant, PackingRatio> execute(final String logisticCenterId, final Instant dateFrom, final Instant dateTo) {
    final Map<Instant, RatioInputGroup> ratioInputGroup = getRatioInputGroupByDate(logisticCenterId, dateFrom, dateTo);
    return ratioInputGroup.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> calculateRatiosByDate(entry.getValue(), dateFrom), (oldValue, newValue) -> oldValue, LinkedHashMap::new)
        );
  }

  private Map<Instant, RatioInputGroup> getRatioInputGroupByDate(
      final String logisticCenterId,
      final Instant dateFrom,
      final Instant dateTo
  ) {
    final ZonedDateTime zonedDateFrom = ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC);
    final ZonedDateTime zonedDateTo = ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC);

    final Map<Integer, List<Ratio>> groups = LongStream.rangeClosed(1, DEFAULT_WEEKS)
        .mapToObj(i -> getRatios(logisticCenterId, dateFrom.minus(i * DAYS_IN_A_WEEK, DAYS), dateTo.minus(i * DAYS_IN_A_WEEK, DAYS)))
        .flatMap(List::stream)
        .collect(Collectors.groupingBy(GetPackingWallRatiosService::groupingKey));

    return LongStream.range(0, HOURS.between(zonedDateFrom, zonedDateTo) + 1)
        .mapToObj(zonedDateFrom::plusHours)
        .collect(
            Collectors.toMap(
                ZonedDateTime::toInstant,
                date -> completeRatioInputGroup(groups.getOrDefault(groupingKey(date), emptyList())))
        );
  }

  private RatioInputGroup completeRatioInputGroup(final List<Ratio> ratioList) {

    final List<UnitsPerDate> unitsPerDatePackingTote = new ArrayList<>();
    final List<UnitsPerDate> unitsPerDatePackingWall = new ArrayList<>();

    ratioList.forEach(ratios -> {
      unitsPerDatePackingTote.add(new UnitsPerDate(ratios.getDate(), 1 - ratios.getValue()));
      unitsPerDatePackingWall.add(new UnitsPerDate(ratios.getDate(), ratios.getValue()));
    });

    return new RatioInputGroup(
        unitsPerDatePackingTote,
        unitsPerDatePackingWall
    );
  }

  private List<Ratio> getRatios(
      final String logisticCenterId,
      final Instant dateFrom,
      final Instant dateTo
  ) {
    try {
      return ratiosRepository.findRatiosByLogisticCenterIdAndDateBetweenAndType(
          logisticCenterId,
          dateFrom,
          dateTo,
          PW_RATIOS_TYPE);
    } catch (Exception e) {
      log.error(e.getMessage());
      return emptyList();
    }
  }

  @Value
  static class RatioInputGroup {
    List<UnitsPerDate> packingToteUnits;

    List<UnitsPerDate> packingWallUnits;
  }

  @Value
  static class UnitsPerDate {
    Instant date;

    double quantity;
  }
}
