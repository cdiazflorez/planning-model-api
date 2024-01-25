package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.Options.APPLY_DATE_OUT_FILTERING;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.Options.VIEW_DATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.Input;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.step.DateOutFilterBuilder.DateOutsGateway;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DateOutFilterBuilderTest {

  private static final String NETWORK_NODE = "nn";

  private static final String[] DATES = {
      "2024-01-22T00:00:00Z",
      "2024-01-22T01:00:00Z",
      "2024-01-22T01:30:00Z",
      "2024-01-23T00:00:00Z",
      "2024-01-23T01:00:00Z",
      "2024-01-23T01:30:00Z",
      "2024-01-24T00:00:00Z",
      "2024-01-22T18:00:00Z",
  };

  private static final List<TaggedUnit> TAGGED_UNITS = List.of(
      new TaggedUnit(1D, tags(DATES[0], DATES[1])),
      new TaggedUnit(2D, tags(DATES[0], DATES[1])),
      new TaggedUnit(3D, tags(DATES[1], DATES[2])),
      new TaggedUnit(4D, tags(DATES[1], DATES[3])),
      new TaggedUnit(5D, tags(DATES[2], DATES[3])),
      new TaggedUnit(1D, tags(DATES[3], DATES[4])),
      new TaggedUnit(2D, tags(DATES[3], DATES[4])),
      new TaggedUnit(3D, tags(DATES[4], DATES[5])),
      new TaggedUnit(4D, tags(DATES[4], DATES[6])),
      new TaggedUnit(5D, tags(DATES[5], DATES[6]))
  );

  private static final List<TaggedUnit> EXPECTED = List.of(
      TAGGED_UNITS.get(0),
      TAGGED_UNITS.get(1),
      TAGGED_UNITS.get(3),
      TAGGED_UNITS.get(4),
      TAGGED_UNITS.get(7),
      TAGGED_UNITS.get(8),
      TAGGED_UNITS.get(9)
  );

  private static final List<Instant> DATE_OUTS_TO_FILTER = List.of(
      Instant.parse(DATES[2]),
      Instant.parse(DATES[4])
  );

  @InjectMocks
  private DateOutFilterBuilder dateOutFilterBuilder;

  @Mock
  private DateOutsGateway dateOutsGateway;

  private static Map<String, String> tags(final String dateIn, final String dateOut) {
    return Map.of(
        Tags.DATE_IN, dateIn,
        Tags.DATE_OUT, dateOut
    );
  }

  public static Stream<Arguments> doNotApplyFilterOptions() {
    return Stream.of(
        Arguments.of(Map.of()),
        Arguments.of(Map.of(VIEW_DATE, DATES[7])),
        Arguments.of(Map.of(APPLY_DATE_OUT_FILTERING, "false")),
        Arguments.of(Map.of(APPLY_DATE_OUT_FILTERING, "false", VIEW_DATE, DATES[7]))
    );
  }

  public static Stream<Arguments> applyFilter() {
    return Stream.of(
        Arguments.of(List.of(), TAGGED_UNITS),
        Arguments.of(DATE_OUTS_TO_FILTER, EXPECTED),
        Arguments.of(List.of(Instant.parse("2024-01-01T18:00:00Z")), TAGGED_UNITS)
    );
  }

  @ParameterizedTest
  @MethodSource("doNotApplyFilterOptions")
  void testShouldNotApplyFilter(final Map<String, String> options) {
    // GIVEN
    final var input = new Input(NETWORK_NODE, FBM_WMS_OUTBOUND, options);

    // WHEN
    final var filter = dateOutFilterBuilder.build(input);
    final var result = filter.apply(TAGGED_UNITS.stream());

    // THEN
    Mockito.verify(dateOutsGateway, Mockito.never())
        .getDateOutsToFilter(Mockito.anyString(), Mockito.any(), Mockito.any());

    assertEquals(TAGGED_UNITS, result.toList());
  }

  @ParameterizedTest
  @MethodSource("applyFilter")
  void testShouldApplyFilter(final List<Instant> toBeFiltered, final List<TaggedUnit> expected) {
    // GIVEN
    final var input = new Input(
        NETWORK_NODE,
        FBM_WMS_OUTBOUND,
        Map.of(
            APPLY_DATE_OUT_FILTERING, "true",
            VIEW_DATE, DATES[7]
        )
    );

    when(dateOutsGateway.getDateOutsToFilter(NETWORK_NODE, FBM_WMS_OUTBOUND, Instant.parse(DATES[7])))
        .thenReturn(toBeFiltered);

    // WHEN
    final var filter = dateOutFilterBuilder.build(input);
    final var result = filter.apply(TAGGED_UNITS.stream());

    // THEN
    assertEquals(expected, result.toList());
  }

}
