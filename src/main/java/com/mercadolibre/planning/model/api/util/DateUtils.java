package com.mercadolibre.planning.model.api.util;

import java.time.ZonedDateTime;
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
    private static final long ONE_HOUR = 1L;

    public static ZonedDateTime getCurrentUtcDate() {
        return ZonedDateTime.now(UTC);
    }

    public static ZonedDateTime fromDate(final Date date) {
        return ofInstant(date.toInstant(), UTC);
    }

    public static Set<String> getForecastWeeks(final ZonedDateTime dateFrom,
                                               final ZonedDateTime dateTo) {
        final Set<String> weeksToConsider = new HashSet<>();

        if (HOURS.between(dateFrom, dateTo) <= ONE_HOUR) {
            weeksToConsider.add(toWeekYear(dateFrom));
            weeksToConsider.add(toWeekYear(dateTo));
            return weeksToConsider;
        }

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

    public static String toWeekYear(final ZonedDateTime zonedDateTime) {
        // TODO: Convert date with warehouse's zone id to get real week
        return String.format("%s-%s",
                zonedDateTime.get(WeekFields.SUNDAY_START.weekOfWeekBasedYear())
                        - HOW_THEY_MANAGE_WEEKS,
                zonedDateTime.get(WeekFields.SUNDAY_START.weekBasedYear()));
    }
}
