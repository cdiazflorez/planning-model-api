package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.Options.TAG_BY;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.Tags.DATE_IN;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.Tags.DATE_OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.Input;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GroupByBuilderTest {

  private static final String[] DATE_INS = {
      "date_in_1",
      "date_in_2",
      "date_in_3",
      "date_in_4",
      "date_in_5",
  };

  private static final String[] DATE_OUTS = {
      "date_out_1",
      "date_out_2",
      "date_out_3",
      "date_out_4",
      "date_out_5",
  };

  private static final String[] PROCESS_PATHS = {
      "process_path_1",
      "process_path_2",
      "process_path_3",
      "process_path_4",
      "process_path_5",
  };

  private static final String DIMENSION = "dimension";

  private static final String EMPTY_STRING = "";

  private static final String PROCESS_PATH = "process_path";

  private static final Set<TaggedUnit> TAGGED_UNITS = Set.of(
      new TaggedUnit(11D, tags(DATE_INS[0], DATE_OUTS[0], PROCESS_PATHS[0])),
      new TaggedUnit(21D, tags(DATE_INS[0], DATE_OUTS[1], PROCESS_PATHS[1])),
      new TaggedUnit(31D, tags(DATE_INS[0], DATE_OUTS[1], PROCESS_PATHS[2])),
      new TaggedUnit(41D, tags(DATE_INS[1], DATE_OUTS[2], PROCESS_PATHS[2])),
      new TaggedUnit(51D, tags(DATE_INS[1], DATE_OUTS[2], PROCESS_PATHS[3])),
      new TaggedUnit(61D, tags(DATE_INS[2], DATE_OUTS[3], PROCESS_PATHS[3])),
      new TaggedUnit(71D, tags(DATE_INS[2], DATE_OUTS[3], PROCESS_PATHS[4])),
      new TaggedUnit(81D, tags(DATE_INS[3], DATE_OUTS[4], PROCESS_PATHS[4])),
      new TaggedUnit(91D, tags(DATE_INS[3], DATE_OUTS[4], PROCESS_PATHS[0])),
      new TaggedUnit(10D, tags(DATE_INS[4], DATE_OUTS[4], PROCESS_PATHS[4]))
  );

  private static Map<String, String> tags(final String dateIn, final String dateOut, final String pp) {
    return Map.of(
        DATE_IN, dateIn,
        DATE_OUT, dateOut,
        PROCESS_PATH, pp
    );
  }

  public static Stream<Arguments> arguments() {
    return Stream.of(
        Arguments.of(
            Map.of(), TAGGED_UNITS
        ),
        Arguments.of(
            Map.of(TAG_BY, DIMENSION),
            Set.of(
                new TaggedUnit(469D, Map.of(DIMENSION, EMPTY_STRING))
            )
        ),
        Arguments.of(
            Map.of(TAG_BY, DATE_IN),
            Set.of(
                new TaggedUnit(63D, Map.of(DATE_IN, DATE_INS[0])),
                new TaggedUnit(92D, Map.of(DATE_IN, DATE_INS[1])),
                new TaggedUnit(132D, Map.of(DATE_IN, DATE_INS[2])),
                new TaggedUnit(172D, Map.of(DATE_IN, DATE_INS[3])),
                new TaggedUnit(10D, Map.of(DATE_IN, DATE_INS[4]))
            )
        ),
        Arguments.of(
            Map.of(TAG_BY, "date_out,process_path"),
            Set.of(
                new TaggedUnit(11D, Map.of(DATE_OUT, DATE_OUTS[0], PROCESS_PATH, PROCESS_PATHS[0])),
                new TaggedUnit(21D, Map.of(DATE_OUT, DATE_OUTS[1], PROCESS_PATH, PROCESS_PATHS[1])),
                new TaggedUnit(31D, Map.of(DATE_OUT, DATE_OUTS[1], PROCESS_PATH, PROCESS_PATHS[2])),
                new TaggedUnit(41D, Map.of(DATE_OUT, DATE_OUTS[2], PROCESS_PATH, PROCESS_PATHS[2])),
                new TaggedUnit(51D, Map.of(DATE_OUT, DATE_OUTS[2], PROCESS_PATH, PROCESS_PATHS[3])),
                new TaggedUnit(61D, Map.of(DATE_OUT, DATE_OUTS[3], PROCESS_PATH, PROCESS_PATHS[3])),
                new TaggedUnit(71D, Map.of(DATE_OUT, DATE_OUTS[3], PROCESS_PATH, PROCESS_PATHS[4])),
                new TaggedUnit(91D, Map.of(DATE_OUT, DATE_OUTS[4], PROCESS_PATH, PROCESS_PATHS[4])),
                new TaggedUnit(91D, Map.of(DATE_OUT, DATE_OUTS[4], PROCESS_PATH, PROCESS_PATHS[0]))
            )
        ),
        Arguments.of(
            Map.of(TAG_BY, "date_in,date_out,process_path"), TAGGED_UNITS
        ),
        Arguments.of(
            Map.of(TAG_BY, "date_in,dimension"),
            Set.of(
                new TaggedUnit(63D, Map.of(DATE_IN, DATE_INS[0], DIMENSION, EMPTY_STRING)),
                new TaggedUnit(92D, Map.of(DATE_IN, DATE_INS[1], DIMENSION, EMPTY_STRING)),
                new TaggedUnit(132D, Map.of(DATE_IN, DATE_INS[2], DIMENSION, EMPTY_STRING)),
                new TaggedUnit(172D, Map.of(DATE_IN, DATE_INS[3], DIMENSION, EMPTY_STRING)),
                new TaggedUnit(10D, Map.of(DATE_IN, DATE_INS[4], DIMENSION, EMPTY_STRING))
            )
        )
    );
  }

  @ParameterizedTest
  @MethodSource("arguments")
  void testGroupBy(
      final Map<String, String> options,
      final Set<TaggedUnit> expected
  ) {
    // GIVEN
    final var builder = new GroupByBuilder();
    final var input = new Input(
        "network_node",
        FBM_WMS_OUTBOUND,
        options
    );

    // WHEN
    final var f = builder.build(input);
    final var result = f.apply(TAGGED_UNITS.stream());

    // THEN
    assertEquals(expected, result.collect(Collectors.toSet()));
  }

}
