package com.mercadolibre.planning.model.api.util;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.WEEKS;

public final class DateUtils {

    public static ZonedDateTime getCurrentUtcDate() {
        return ZonedDateTime.now(UTC);
    }

    public static ZonedDateTime fromDate(final Date date) {
        return ofInstant(date.toInstant(), UTC);
    }

    public static Set<String> getForecastWeeks(final ZonedDateTime dateFrom,
                                               final ZonedDateTime dateTo) {

        final long weeks = (int) WEEKS.between(dateFrom, dateTo);

        return LongStream.rangeClosed(0, weeks).boxed()
                .map(integer -> toWeekYear(dateFrom.plusWeeks(integer)))
                .collect(Collectors.toSet());
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
                utcTimestamp.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                utcTimestamp.get(IsoFields.WEEK_BASED_YEAR));
    }
}
