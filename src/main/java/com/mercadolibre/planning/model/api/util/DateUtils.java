package com.mercadolibre.planning.model.api.util;

import java.time.OffsetTime;
import java.time.ZoneOffset;

public final class DateUtils {

    public static OffsetTime getUtcOffset(final OffsetTime offsetTime) {
        return offsetTime.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
