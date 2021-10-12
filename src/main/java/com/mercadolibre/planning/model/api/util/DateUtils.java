package com.mercadolibre.planning.model.api.util;

import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Stream.iterate;

public final class DateUtils {

    private static final int HOW_THEY_MANAGE_WEEKS_2021 = 1;

    public static ZonedDateTime getCurrentUtcDate() {
        return ZonedDateTime.now(UTC).truncatedTo(SECONDS);
    }

    public static ZonedDateTime fromDate(final Date date) {
        return ofInstant(date.toInstant(), UTC);
    }

    public static Set<String> getForecastWeeks(final ZonedDateTime dateFrom,
                                               final ZonedDateTime dateTo) {

        final Set<String> weeksToConsider = new HashSet<>();
        iterate(dateFrom.minusDays(1), date -> date.plusHours(1))
                .limit(HOURS.between(dateFrom.minusDays(1), dateTo.plusDays(1)))
                .forEach(dateTime -> weeksToConsider.add(toWeekYear(dateTime)));
        return weeksToConsider;
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
}
