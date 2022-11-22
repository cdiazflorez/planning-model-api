package com.mercadolibre.planning.model.api.util;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Stream.iterate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public final class DateUtils {
  public static final double MINUTES_IN_HOUR = 60.0;

  private static final int HOW_THEY_MANAGE_WEEKS_2021 = 1;

  private DateUtils() {
  }

  public static ZonedDateTime getCurrentUtcDate() {
    return ZonedDateTime.now(UTC).truncatedTo(SECONDS);
  }

  public static ZonedDateTime fromDate(final Date date) {
    return ofInstant(date.toInstant(), UTC);
  }

  public static ZonedDateTime fromInstant(final Instant date) {
    return ofInstant(date, UTC);
  }

  public static Set<String> getForecastWeeks(final ZonedDateTime dateFrom,
                                             final ZonedDateTime dateTo) {

    final Set<String> weeksToConsider = new HashSet<>();
    iterate(dateFrom.minusDays(1), date -> date.plusHours(1))
        .limit(HOURS.between(dateFrom.minusDays(1), dateTo.plusDays(1)))
        .forEach(dateTime -> weeksToConsider.add(toWeekYear(dateTime)));
    return weeksToConsider;
  }

  public static boolean isBetweenInclusive(final ChronoZonedDateTime<LocalDate> date,
                                           final ChronoZonedDateTime<LocalDate> from,
                                           final ChronoZonedDateTime<LocalDate> to) {

    return date.equals(from) || date.equals(to) || date.isAfter(from) && date.isBefore(to);
  }

  public static boolean isBetweenInclusive(final ChronoZonedDateTime<LocalDate> lower,
                                           final Instant probe,
                                           final ChronoZonedDateTime<LocalDate> higher) {
    return !probe.isBefore(lower.toInstant()) && !higher.toInstant().isBefore(probe);
  }

  private static String toWeekYear(final ZonedDateTime dateTime) {
    final int week = dateTime.get(WeekFields.SUNDAY_START.weekOfWeekBasedYear());
    final int year = dateTime.get(WeekFields.SUNDAY_START.weekBasedYear());
    return year == 2021
        ? format("%s-%s", week - HOW_THEY_MANAGE_WEEKS_2021, year)
        : format("%s-%s", week, year);
  }

  public static ZonedDateTime ignoreMinutes(final ZonedDateTime dateTime) {
    return dateTime.truncatedTo(HOURS).withFixedOffsetZone();
  }

  public static ZonedDateTime nextHour(final ZonedDateTime dateTime) {
    return dateTime.truncatedTo(HOURS).plusHours(1);
  }

  public static Stream<Instant> instantRange(final Instant from, final Instant to, final ChronoUnit step) {
    final Stream<Instant> first = Stream.of(from);

    final var end = to.truncatedTo(step);
    final Stream<Instant> last = end.equals(to) ? Stream.empty() : Stream.of(end);

    final var start = from.truncatedTo(step);
    final var steps = step.between(start, end) + 1L;
    final Stream<Instant> middle = LongStream.range(1, steps)
        .mapToObj(shift -> start.plus(shift, step));


    return Stream.of(first, middle, last)
        .flatMap(Function.identity());
  }

  public static String getActualForecastWeek(final ZonedDateTime dateTime) {
    return toWeekYear(dateTime);
  }

  public static Optional<Instant> max(final Instant one, final Instant other) {
    final var x = Optional.ofNullable(one);
    final var y = Optional.ofNullable(other);

    return x.isEmpty() ? y : y.isEmpty() ? x : x.get().isBefore(y.get()) ? y : x;
  }

  public static Optional<Instant> min(final Instant one, final Instant other) {
    final var x = Optional.ofNullable(one);
    final var y = Optional.ofNullable(other);

    return x.isEmpty() ? y : y.isEmpty() ? x : x.get().isBefore(y.get()) ? x : y;
  }
}
