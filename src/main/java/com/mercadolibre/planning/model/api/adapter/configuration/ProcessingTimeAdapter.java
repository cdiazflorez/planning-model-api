package com.mercadolibre.planning.model.api.adapter.configuration;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.api.client.db.repository.configuration.OutboundProcessingTimeRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.OutboundProcessingTime;
import com.mercadolibre.planning.model.api.domain.service.configuration.DayAndHourProcessingTime;
import com.mercadolibre.planning.model.api.domain.service.configuration.ProcessingTimeService.OutboundProcessingTimeGateway;
import com.mercadolibre.planning.model.api.domain.service.configuration.SlaProcessingTimes.SlaProperties;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.DayDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;
import com.mercadolibre.planning.model.api.exception.ProcessingTimeException;
import com.mercadolibre.planning.model.api.gateway.RouteEtsGateway;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ProcessingTimeAdapter implements OutboundProcessingTimeGateway {

  private static final int MINUTES_IN_HOUR = 60;

  private static final String ACTIVE = "active";

  private static final int ONE_DAY = 1;

  private static final int HOUR_TO_PATTERN_CONSTANT = 100;

  private static final int DAYS_TO_PURGE_BEFORE = 30;

  private final OutboundProcessingTimeRepository outboundProcessingTimeRepository;

  private final RouteEtsGateway routeEtsGateway;

  /**
   * Retrieves a Stream of SlaProperties from repo response based on the provided values.
   *
   * @param logisticCenterId The ID of the logistic center.
   * @param dateFrom         The starting date and time of the range.
   * @param dateTo           The ending date and time of the range.
   * @param zoneId           The time zone of the date range.
   * @return A stream of SlaProperties objects within the specified range and logistic center.
   * @throws ProcessingTimeException if something happens when executing a repo method.
   */
  @Override
  public Stream<SlaProperties> getOutboundProcessingTimeByCptInRange(
      final String logisticCenterId,
      final Instant dateFrom,
      final Instant dateTo,
      final ZoneId zoneId
  ) {

    final List<OutboundProcessingTime> processingTimes;
    try {
      processingTimes = outboundProcessingTimeRepository.findByLogisticCenterAndIsActive(logisticCenterId);
    } catch (final DataAccessException dae) {
      throw new ProcessingTimeException(dae.getMessage(), dae);
    }

    final ZonedDateTime zonedDateTimeFrom = ZonedDateTime.ofInstant(dateFrom, zoneId);
    final ZonedDateTime zonedDateTimeTo = ZonedDateTime.ofInstant(dateTo, zoneId);
    final Set<String> daysOfWeek = getDaysOfWeek(zonedDateTimeFrom, zonedDateTimeTo);

    final List<OutboundProcessingTime> processingTimesInDays = processingTimes.stream()
        .filter(processingTime -> daysOfWeek.contains(processingTime.getEtdDay().toLowerCase(Locale.US)))
        .toList();

    return getSlaPropertiesInRangeByDayAndHour(processingTimesInDays, zonedDateTimeFrom, zonedDateTimeTo);
  }

  /**
   * Retrieves a list of DayAndHourProcessingTime objects based on active routes from the route client.
   *
   * @param logisticCenterId The ID of the logistic center.
   * @return A list of DayAndHourProcessingTime objects corresponding to the active routes of the logistic center.
   * @throws ProcessingTimeException if something happens when executing the Api Client request.
   */
  public List<DayAndHourProcessingTime> getOutboundProcessingTimeByLogisticCenterFromRouteClient(final String logisticCenterId) {

    final List<RouteEtsDto> etsRoutes;
    try {
      etsRoutes = routeEtsGateway.postRoutEts(
          RouteEtsRequest.builder()
              .fromFilter(List.of(logisticCenterId))
              .build()
      );
    } catch (final ClientException ce) {
      throw new ProcessingTimeException(ce.getMessage(), ce);
    }

    final List<RouteEtsDto> activeRoutes = etsRoutes.stream()
        .filter(route -> ACTIVE.equals(route.getStatusRoute()))
        .toList();

    return getSortedOutboundProcessingTimesByDayAndHour(activeRoutes, logisticCenterId);
  }

  /**
   * Updates DayAndHourProcessingTime records for a logistic center with the provided processing times.
   *
   * @param logisticCenterId The ID of the logistic center.
   * @param processingTimes  The list of OutboundProcessingTime objects to update.
   * @return A list of updated DayAndHourProcessingTime objects for the logistic center.
   * @throws ProcessingTimeException if something happens when executing a repo method.
   */
  public List<DayAndHourProcessingTime> updateOutboundProcessingTimesForLogisticCenter(
      final String logisticCenterId,
      final List<DayAndHourProcessingTime> processingTimes
  ) {
    final List<OutboundProcessingTime> processingTimesToSave = processingTimes.stream()
        .map(DayAndHourProcessingTime::toActiveOutboundProcessingTime)
        .toList();

    final List<OutboundProcessingTime> savedRegisters;
    try {
      outboundProcessingTimeRepository.purgeOldRecords(Instant.now().plus(DAYS_TO_PURGE_BEFORE, ChronoUnit.DAYS));
      outboundProcessingTimeRepository.deactivateAllByLogisticCenter(logisticCenterId);
      savedRegisters = outboundProcessingTimeRepository.saveAll(processingTimesToSave);
    } catch (final DataAccessException dae) {
      throw new ProcessingTimeException(dae.getMessage(), dae);
    }

    return savedRegisters.stream()
        .map(DayAndHourProcessingTime::new)
        .sorted(Comparator
            .comparing(DayAndHourProcessingTime::getDayOfWeek)
            .thenComparing(DayAndHourProcessingTime::etdHour)
        ).toList();
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

  /**
   * Filters a list of OutboundProcessingTime based on a specified date range.
   * Separates the items into those from the starting day, the ending day, and those in between, and then concatenates the results.
   *
   * @param processingTimes The list of OutboundProcessingTimeData objects to filter.
   * @param dateFrom        The starting date and time of the range.
   * @param dateTo          The ending date and time of the range.
   * @return A filtered stream of SlaProperties.
   */
  private Stream<SlaProperties> getSlaPropertiesInRangeByDayAndHour(
      final List<OutboundProcessingTime> processingTimes,
      final ZonedDateTime dateFrom,
      final ZonedDateTime dateTo
  ) {

    final Stream<OutboundProcessingTime> processingTimesInDateRange = filterProcessingTimesInDateRange(processingTimes, dateFrom, dateTo);

    return processingTimesInDateRange
        .map(processingTime -> toSlaProperties(processingTime, dateFrom.getZone(), dateFrom));
  }

  private Stream<OutboundProcessingTime> filterProcessingTimesInDateRange(
      final List<OutboundProcessingTime> processingTimes,
      final ZonedDateTime dateFrom,
      final ZonedDateTime dateTo
  ) {

    final String dayFrom = getDayNameFromZonedDateTime(dateFrom);
    final String dayTo = getDayNameFromZonedDateTime(dateTo);
    final int hourFrom = toIntegerFromZonedDateTimeHour(dateFrom);
    final int hourTo = toIntegerFromZonedDateTimeHour(dateTo);

    if (dayFrom.equals(dayTo)) {
      return filterByDayAndMaxHourSameDay(processingTimes, dayFrom, hourFrom, hourTo);
    }

    final Stream<OutboundProcessingTime> processingTimesByDayAndHourFrom = filterByDayAndMinHour(processingTimes, dayFrom, hourFrom);
    final Stream<OutboundProcessingTime> daysBetweenFromAndTo = filterOuterDays(processingTimes, Set.of(dayFrom, dayTo));
    final Stream<OutboundProcessingTime> processingTimesByDayAndHourTo = filterByDayAndMaxHour(processingTimes, dayTo, hourTo);

    return Stream.concat(
        Stream.concat(processingTimesByDayAndHourFrom, daysBetweenFromAndTo),
        processingTimesByDayAndHourTo
    );
  }

  private List<DayAndHourProcessingTime> getSortedOutboundProcessingTimesByDayAndHour(
      final List<RouteEtsDto> activeRoutes,
      final String logisticCenterId
  ) {

    final Collection<DayAndHourProcessingTime> distinctProcessingTimesByDay = createDistinctProcessingTimesByDayMapFromRouteDto(
        activeRoutes,
        logisticCenterId
    );

    return distinctProcessingTimesByDay.stream()
        .sorted(Comparator
            .comparing(DayAndHourProcessingTime::getDayOfWeek)
            .thenComparing(DayAndHourProcessingTime::etdHour)
        )
        .toList();
  }

  private String getDayNameFromZonedDateTime(final ZonedDateTime zonedDateTime) {
    return zonedDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.US).toLowerCase(Locale.US);
  }

  /**
   * Converts the hour and minute components of a ZonedDateTime into an integer representation. "12:15" -> 1215
   *
   * @param zonedDateTime The ZonedDateTime from which to extract the hour and minute.
   * @return An integer representation in the format HHmm.
   */
  private int toIntegerFromZonedDateTimeHour(final ZonedDateTime zonedDateTime) {
    return zonedDateTime.getHour() * HOUR_TO_PATTERN_CONSTANT + zonedDateTime.getMinute();
  }

  private Stream<OutboundProcessingTime> filterByDayAndMaxHourSameDay(
      final List<OutboundProcessingTime> processingTimes,
      final String dayTo,
      final int hourFrom,
      final int hourTo
  ) {
    return processingTimes.stream()
        .filter(processingTime ->
            dayTo.equals(processingTime.getEtdDay())
                && hourFrom <= processingTime.getParsedEtdHour()
                && hourTo >= processingTime.getParsedEtdHour()
        );
  }

  private Stream<OutboundProcessingTime> filterByDayAndMinHour(
      final List<OutboundProcessingTime> processingTimes,
      final String dayFrom,
      final int hourFrom
  ) {
    return processingTimes.stream()
        .filter(processingTime -> dayFrom.equals(processingTime.getEtdDay()) && hourFrom <= processingTime.getParsedEtdHour());
  }

  private Stream<OutboundProcessingTime> filterByDayAndMaxHour(
      final List<OutboundProcessingTime> processingTimes,
      final String dayTo,
      final int hourTo
  ) {
    return processingTimes.stream()
        .filter(processingTime -> dayTo.equals(processingTime.getEtdDay()) && hourTo >= processingTime.getParsedEtdHour());
  }

  private Stream<OutboundProcessingTime> filterOuterDays(
      final List<OutboundProcessingTime> processingTimes,
      final Set<String> excludedDays
  ) {
    return processingTimes.stream()
        .filter(processingTime -> !excludedDays.contains(processingTime.getEtdDay()));
  }

  private Collection<DayAndHourProcessingTime> createDistinctProcessingTimesByDayMapFromRouteDto(
      final List<RouteEtsDto> activeRoutes,
      final String logisticCenterId
  ) {

    final Stream<DayDto> allCptDays = activeRoutes.stream()
        .flatMap(route -> route.getFixedEtsByDay().values().stream().filter(Objects::nonNull))
        .flatMap(Collection::stream);

    record Aux(String day, String hour) {
    }

    return allCptDays
        .map(day -> toDayAndHourProcessingTime(day, logisticCenterId))
        .collect(
            Collectors.toMap(
                day -> new Aux(day.etdDay(), day.etdHour()),
                Function.identity(),
                (a, b) -> a.etdProcessingTime() < b.etdProcessingTime() ? a : b
            )
        )
        .values();
  }

  private DayAndHourProcessingTime toDayAndHourProcessingTime(final DayDto dayDto, final String logisticCenterId) {
    final int processingTimeInMinutes = Integer.parseInt(dayDto.getProcessingTime().substring(2))
        + Integer.parseInt(dayDto.getProcessingTime().substring(0, 2))
        * MINUTES_IN_HOUR;

    return new DayAndHourProcessingTime(
        logisticCenterId,
        dayDto.getEtDay(),
        dayDto.getEtHour(),
        processingTimeInMinutes
    );
  }

  private SlaProperties toSlaProperties(
      final OutboundProcessingTime processingTime,
      final ZoneId zoneId,
      final ZonedDateTime dateFrom
  ) {
    final Instant sla = convertDayHourAndZoneIdToInstantInWeek(
        processingTime.getEtdDay(),
        processingTime.getEtdHour(),
        zoneId,
        dateFrom
    );
    return new SlaProperties(
        sla,
        processingTime.getEtdProcessingTime()
    );
  }

  private Instant convertDayHourAndZoneIdToInstantInWeek(
      final String day,
      final String hour,
      final ZoneId zoneId,
      final ZonedDateTime dateFrom
  ) {
    final DayOfWeek targetDayOfWeek = DayOfWeek.valueOf(day.toUpperCase(Locale.US));

    final LocalDate currentDate = dateFrom.toLocalDate()
        .with(TemporalAdjusters.nextOrSame(targetDayOfWeek));

    final LocalDateTime targetDateTime = LocalDateTime.of(currentDate, LocalTime.parse(hour, DateTimeFormatter.ofPattern("HHmm")));

    return targetDateTime.atZone(zoneId).toInstant();
  }
}
