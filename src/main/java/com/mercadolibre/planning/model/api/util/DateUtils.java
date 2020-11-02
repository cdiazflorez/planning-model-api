package com.mercadolibre.planning.model.api.util;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.time.temporal.ChronoUnit.WEEKS;

public final class DateUtils {

    public static OffsetTime getUtcOffset(final OffsetTime offsetTime) {
        return offsetTime.withOffsetSameInstant(ZoneOffset.UTC);
    }

    public static Set<String> getForecastWeeks(final ZonedDateTime dateFrom,
                                         final ZonedDateTime dateTo) {
        final DateTimeFormatter weekFormat = DateTimeFormatter.ofPattern("w-YYYY");
        final long weeks = (int) WEEKS.between(dateFrom, dateTo);

        return LongStream.rangeClosed(0, weeks).boxed()
                .map(integer -> dateFrom.plusWeeks(integer).format(weekFormat))
                .collect(Collectors.toSet());
    }
}
