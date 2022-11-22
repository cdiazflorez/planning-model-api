package com.mercadolibre.planning.model.api.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DateUtilTest {

  @Test
  void testMinAndMaxNullFirst() {
    var now = Instant.now();
    var tomorrow = now.plus(1, ChronoUnit.DAYS);
    var resMin = DateUtils.min(null, now);
    var resMax = DateUtils.max(null, tomorrow);
    var resMinCompare = DateUtils.min(now, tomorrow);
    var resMaxCompare = DateUtils.max(now, tomorrow);

    Assertions.assertEquals(now, resMin.get());
    Assertions.assertEquals(tomorrow, resMax.get());
    Assertions.assertEquals(now, resMinCompare.get());
    Assertions.assertEquals(tomorrow, resMaxCompare.get());
  }

  @Test
  void testMinAndMaxNullSecond() {
    var now = Instant.now();
    var tomorrow = now.plus(1, ChronoUnit.DAYS);

    var resMin = DateUtils.min(now, null);
    var resMax = DateUtils.max(tomorrow, null);
    var resMinCompare = DateUtils.min(now, tomorrow);
    var resMaxCompare = DateUtils.max(now, tomorrow);
    var resMinCompare2 = DateUtils.min(tomorrow, now);
    var resMaxCompare2 = DateUtils.max(tomorrow, now);

    Assertions.assertEquals(now, resMin.get());
    Assertions.assertEquals(tomorrow, resMax.get());
    Assertions.assertEquals(now, resMinCompare.get());
    Assertions.assertEquals(tomorrow, resMaxCompare.get());
    Assertions.assertEquals(now, resMinCompare2.get());
    Assertions.assertEquals(tomorrow, resMaxCompare2.get());
  }

  @Test
  void testInstantRange() {
    final Instant date = Instant.parse("2022-11-22T00:00:00Z");

    var response = DateUtils.instantRange(
        Instant.parse("2022-11-22T00:00:00Z"),
        Instant.parse("2022-11-23T00:00:00Z"),
        ChronoUnit.HOURS).findAny().get();

    Assertions.assertEquals(date, response);
  }
}
