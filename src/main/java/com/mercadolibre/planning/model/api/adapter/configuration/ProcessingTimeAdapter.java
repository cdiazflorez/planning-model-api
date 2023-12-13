package com.mercadolibre.planning.model.api.adapter.configuration;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.OutboundProcessingTimeRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.OutboundProcessingTime;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProcessingTimeAdapter {

  private static final String DATE_TIME_PATTERN = "HHmm";

  private static final int ONE_DAY = 1;

  private final OutboundProcessingTimeRepository outboundProcessingTimeRepository;

  /**
   * Retrieves a list of OutboundProcessingTime from repo based on the provided values.
   *
   * @param logisticCenterId The ID of the logistic center.
   * @param dateFrom         The starting date and time of the range.
   * @param dateTo           The ending date and time of the range.
   * @param zoneId           The time zone of the date range.
   * @return A list of OutboundProcessingTime objects within the specified range and logistic center.
   */
  public List<EtdProcessingTimeData> getOutboundProcessingTimeByCptInRange(
      final String logisticCenterId,
      final Instant dateFrom,
      final Instant dateTo,
      final ZoneId zoneId
  ) {

    final ZonedDateTime zonedDateTimeFrom = ZonedDateTime.ofInstant(dateFrom, zoneId);
    final ZonedDateTime zonedDateTimeTo = ZonedDateTime.ofInstant(dateTo, zoneId);

    final Set<String> daysOfWeek = getDaysOfWeek(zonedDateTimeFrom, zonedDateTimeTo);

    final List<OutboundProcessingTime> processingTimes = outboundProcessingTimeRepository
        .findByLogisticCenterAndIsActive(logisticCenterId).stream()
        .filter(processingTime -> daysOfWeek.contains(processingTime.getEtdDay().toLowerCase(Locale.US)))
        .toList();

    return filterOutboundProcessingTimesByDayAndHour(processingTimes, zonedDateTimeFrom, zonedDateTimeTo);
  }

  private Set<String> getDaysOfWeek(final ZonedDateTime zonedDateTimeFrom, final ZonedDateTime zonedDateTimeTo) {

    final long numOfDaysBetween = ChronoUnit.DAYS.between(
        zonedDateTimeFrom.truncatedTo(ChronoUnit.DAYS),
        zonedDateTimeTo.truncatedTo(ChronoUnit.DAYS)
    ) + ONE_DAY;

    return Stream.iterate(zonedDateTimeFrom, date -> date.plusDays(ONE_DAY))
        .limit(numOfDaysBetween)
        .map(this::getDayNameFromZonedDateTime)
        .collect(Collectors.toSet());
  }

  private String getDayNameFromZonedDateTime(final ZonedDateTime zonedDateTime) {
    return zonedDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US).toLowerCase(Locale.US);
  }

  private int toIntegerFromZonedDateTimeHour(final ZonedDateTime zonedDateTime) {
    return Integer.parseInt(zonedDateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)));
  }

  /**
   * Filters a list of OutboundProcessingTimeData based on a specified date range.
   * Separates the items into those from the starting day, the ending day, and those in between, and then concatenates the results.
   *
   * @param processingTimes The list of OutboundProcessingTimeData objects to filter.
   * @param dateFrom        The starting date and time of the range.
   * @param dateTo          The ending date and time of the range.
   * @return A filtered list of OutboundProcessingTime.
   */
  private List<EtdProcessingTimeData> filterOutboundProcessingTimesByDayAndHour(
      final List<OutboundProcessingTime> processingTimes,
      final ZonedDateTime dateFrom,
      final ZonedDateTime dateTo
  ) {
    final String dayFrom = getDayNameFromZonedDateTime(dateFrom);
    final String dayTo = getDayNameFromZonedDateTime(dateTo);
    final int hourFrom = toIntegerFromZonedDateTimeHour(dateFrom);
    final int hourTo = toIntegerFromZonedDateTimeHour(dateTo);

    final var processingTimesByDayAndHourFrom = filterByDayAndMinHour(processingTimes, dayFrom, hourFrom);

    final var daysBetweenFromAndTo = filterOuterDays(processingTimes, Set.of(dayFrom, dayTo));

    final var processingTimesByDayAndHourTo = filterByDayAndMaxHour(processingTimes, dayTo, hourTo);

    return toOutboundProcessingTime(Stream.concat(
        Stream.concat(processingTimesByDayAndHourFrom, daysBetweenFromAndTo),
        processingTimesByDayAndHourTo
    ));
  }


  private List<EtdProcessingTimeData> toOutboundProcessingTime(final Stream<OutboundProcessingTime> processingTimeData) {
    return processingTimeData
        .map(EtdProcessingTimeData::new)
        .sorted(Comparator
            .comparing(EtdProcessingTimeData::getDayOfWeek)
            .thenComparing(EtdProcessingTimeData::etdHour)
        )
        .toList();
  }

  private Stream<OutboundProcessingTime> filterByDayAndMinHour(
      final List<OutboundProcessingTime> processingTimes,
      final String dayFrom,
      final int hourFrom
  ) {
    return processingTimes.stream()
        .filter(processingTime -> dayFrom.equals(processingTime.getEtdDay()) && hourFrom <= Integer.parseInt(processingTime.getEtdHour()));
  }

  private Stream<OutboundProcessingTime> filterByDayAndMaxHour(
      final List<OutboundProcessingTime> processingTimes,
      final String dayTo,
      final int hourTo
  ) {
    return processingTimes.stream()
        .filter(processingTime -> dayTo.equals(processingTime.getEtdDay()) && hourTo >= Integer.parseInt(processingTime.getEtdHour()));
  }

  private Stream<OutboundProcessingTime> filterOuterDays(
      final List<OutboundProcessingTime> processingTimes,
      final Set<String> excludedDays
  ) {
    return processingTimes.stream()
        .filter(processingTime -> !excludedDays.contains(processingTime.getEtdDay()));
  }

  public record EtdProcessingTimeData(
      String logisticCenterID,
      String etdDay,
      String etdHour,
      int etdProcessingTime
  ) {
    public EtdProcessingTimeData(OutboundProcessingTime data) {
      this(data.getLogisticCenterID(), data.getEtdDay(), data.getEtdHour(), data.getEtdProcessingTime());
    }

    public DayOfWeek getDayOfWeek() {
      return DayOfWeek.valueOf(etdDay.toUpperCase(Locale.US));
    }
  }
}
