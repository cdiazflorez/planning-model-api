package com.mercadolibre.planning.model.api.domain.entity.deviation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DeviationTest {

  private static final String DATE_1 = "2022-01-01T00:00:00Z";
  private static final String DATE_2 = "2023-01-01T00:00:00Z";
  private static final String DATE_3 = "2022-01-15T00:00:00Z";
  private static final String DATE_4 = "2022-01-31T00:00:00Z";
  private static final String DATE_5 = "2022-01-01T01:00:00Z";
  private static final String DATE_6 = "2022-02-01T00:00:00Z";
  private static final String DATE_IN = "date_in";
  private static final String DATE_IN_FROM = "date_in_from";
  private static final String DATE_OUT = "date_out";
  private static final String DATE_OUT_FROM = "date_out_from";
  private static final String DATE_OUT_TO = "date_out_to";
  private static final String NON_TOT_MONO = "non_tot_mono";
  private static final String PROCESS_PATH = "process_path";
  private static final String TOT_MONO = "tot_mono";

  private static Stream<Arguments> provideDeviationTestData() {
    return Stream.of(
        Arguments.of(new Deviation(0.5, Map.of(DATE_OUT_FROM, DATE_1)), new TaggedUnit(100, Map.of(DATE_OUT, DATE_1)),
            new TaggedUnit(150, Map.of(DATE_OUT, DATE_1))),
        Arguments.of(new Deviation(0.5, Map.of(DATE_OUT_FROM, DATE_1)), new TaggedUnit(100, Map.of(DATE_OUT, DATE_2)),
            new TaggedUnit(150, Map.of(DATE_OUT, DATE_2))),
        Arguments.of(new Deviation(0.5, Map.of(DATE_IN_FROM, DATE_1, DATE_OUT_TO, DATE_6)),
            new TaggedUnit(100, Map.of(DATE_IN, DATE_3, DATE_OUT, DATE_4)), new TaggedUnit(150, Map.of(DATE_IN, DATE_3, DATE_OUT, DATE_4))),
        Arguments.of(new Deviation(0.2, Map.of(DATE_IN_FROM, DATE_5, DATE_OUT_FROM, DATE_1)),
            new TaggedUnit(100, Map.of(DATE_IN, DATE_1, DATE_OUT, DATE_1)), new TaggedUnit(100, Map.of(DATE_IN, DATE_1, DATE_OUT, DATE_1))),
        Arguments.of(new Deviation(0.5, Map.of(PROCESS_PATH, TOT_MONO)), new TaggedUnit(100, Map.of(PROCESS_PATH, TOT_MONO)),
            new TaggedUnit(150, Map.of(PROCESS_PATH, TOT_MONO))),
        Arguments.of(new Deviation(0.2, Map.of(PROCESS_PATH, NON_TOT_MONO)), new TaggedUnit(100, Map.of(PROCESS_PATH, TOT_MONO)),
            new TaggedUnit(100, Map.of(PROCESS_PATH, TOT_MONO))),
        Arguments.of(new Deviation(0.2, Map.of(DATE_IN_FROM, DATE_5, PROCESS_PATH, TOT_MONO)),
            new TaggedUnit(100, Map.of(DATE_IN, DATE_1, PROCESS_PATH, TOT_MONO)),
            new TaggedUnit(100, Map.of(DATE_IN, DATE_1, PROCESS_PATH, TOT_MONO))),
        Arguments.of(new Deviation(0.2, Map.of("", DATE_1)),
            new TaggedUnit(100, Map.of(DATE_IN, DATE_1)),
            new TaggedUnit(100, Map.of(DATE_IN, DATE_1)))
    );
  }

  @ParameterizedTest
  @MethodSource("provideDeviationTestData")
  void applyDeviationTest(final Deviation deviation, final TaggedUnit taggedUnit, final TaggedUnit expectedResult) {

    TaggedUnit result = deviation.applyDeviation(taggedUnit);

    assertEquals(expectedResult, result);
  }

}
