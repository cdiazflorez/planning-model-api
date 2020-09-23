package com.mercadolibre.planning.model.api.util;

import org.junit.jupiter.api.Test;

import java.time.OffsetTime;

import static com.mercadolibre.planning.model.api.util.TestUtils.AN_OFFSET_TIME;
import static com.mercadolibre.planning.model.api.util.TestUtils.AN_OFFSET_TIME_UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateUtilsTest {

    @Test
    public void testGetUtcOffset() {
        // WHEN
        final OffsetTime offsetTime = DateUtils.getUtcOffset(AN_OFFSET_TIME);

        // THEN
        assertEquals(AN_OFFSET_TIME_UTC, offsetTime);
    }
}
