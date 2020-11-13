package com.mercadolibre.planning.model.api.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.temporal.ChronoUnit.WEEKS;

public final class DateUtils {

    public static ZonedDateTime fromDate(final Date date) {
        return ofInstant(date.toInstant(), UTC);
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
