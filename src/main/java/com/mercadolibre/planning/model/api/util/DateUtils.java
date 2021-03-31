package com.mercadolibre.planning.model.api.util;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.stream.Stream.iterate;

public final class DateUtils {

    private static final int HOW_THEY_MANAGE_WEEKS = 1;

    public static ZonedDateTime getCurrentUtcDate() {
        return ZonedDateTime.now(UTC);
    }

    public static ZonedDateTime fromDate(final Date date) {
        return ofInstant(date.toInstant(), UTC);
    }

    public static Set<String> getForecastWeeks(final ZonedDateTime dateFrom,
                                               final Temporal dateTo) {
        final Set<String> weeksToConsider = new HashSet<>();

        iterate(dateFrom, date -> date.plusHours(1))
                .limit(HOURS.between(dateFrom, dateTo))
                .forEach(dateTime -> weeksToConsider.add(toWeekYear(dateTime)));

        return weeksToConsider;
    }

    public static ZonedDateTime ignoreMinutes(final ZonedDateTime dateTime) {
        return dateTime.truncatedTo(HOURS).withFixedOffsetZone();
    }

    public static ZonedDateTime nextHour(final ZonedDateTime dateTime) {
        return dateTime.truncatedTo(HOURS).plusHours(1);
    }

    public static String toWeekYear(final TemporalAccessor zonedDateTime) {
        final Instant instant = Instant.from(zonedDateTime);
        final ZonedDateTime utcTimestamp = instant.atZone(UTC);

        return String.format("%s-%s",
                utcTimestamp.get(WeekFields.SUNDAY_START.weekOfWeekBasedYear())
                        - HOW_THEY_MANAGE_WEEKS,
                utcTimestamp.get(WeekFields.SUNDAY_START.weekBasedYear()));
    }
}
