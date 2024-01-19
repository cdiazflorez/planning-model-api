package com.mercadolibre.planning.model.api.adapter.units;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.Input;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService.TaggedUnit;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionGateway;
import com.mercadolibre.planning.model.api.exception.InvalidArgumentException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanningDistributionAdapterTest {

  private static final String OPTION_DATE_IN_FROM = "date_in_from";

  private static final String OPTION_DATE_IN_TO = "date_in_to";

  private static final String OPTION_DATE_OUT_FROM = "date_out_from";

  private static final String OPTION_DATE_OUT_TO = "date_out_to";

  private static final String OPTION_VIEW_DATE = "view_date";

  private static final String OPTION_PROCESS_PATHS = "process_paths";

  private static final String NETWORK_NODE = "ARTW01";

  private static final Instant VIEW_DATE = Instant.parse("2023-01-17T00:00:00Z");

  private static final Instant DATE_IN_FROM = Instant.parse("2023-01-18T00:00:00Z");

  private static final Instant DATE_IN_TO = Instant.parse("2023-01-18T05:00:00Z");

  private static final Instant DATE_OUT_FROM = Instant.parse("2023-01-18T06:00:00Z");

  private static final Instant DATE_OUT_TO = Instant.parse("2023-01-18T10:00:00Z");

  private static final List<PlanDistribution> STORED_PLAN_DISTRIBUTIONS = List.of(
      new PlanDistribution(1L, DATE_IN_FROM, DATE_OUT_FROM, TOT_MONO, UNITS, 10D),
      new PlanDistribution(1L, DATE_IN_FROM, DATE_OUT_TO, TOT_MONO, UNITS, 15D),

      new PlanDistribution(1L, DATE_IN_TO, DATE_OUT_FROM, TOT_MONO, UNITS, 20D),
      new PlanDistribution(2L, DATE_IN_TO, DATE_OUT_FROM, TOT_MONO, UNITS, 30D),

      new PlanDistribution(2L, DATE_IN_TO, DATE_OUT_TO, TOT_MONO, UNITS, 35D)
  );

  private static final List<TaggedUnit> EXPECTED = List.of(
      taggedUnits(10D, DATE_IN_FROM, DATE_OUT_FROM, TOT_MONO),
      taggedUnits(15D, DATE_IN_FROM, DATE_OUT_TO, TOT_MONO),
      taggedUnits(20D, DATE_IN_TO, DATE_OUT_FROM, TOT_MONO),
      taggedUnits(35D, DATE_IN_TO, DATE_OUT_TO, TOT_MONO)
  );

  @InjectMocks
  private PlanningDistributionAdapter adapter;

  @Mock
  private GetForecastUseCase getForecastUseCase;

  @Mock
  private PlanningDistributionGateway planningDistributionGateway;

  private static TaggedUnit taggedUnits(
      final double quantity,
      final Instant dateIn,
      final Instant dateOut,
      final ProcessPath processPath
  ) {
    return new TaggedUnit(
        quantity,
        Map.of(
            "date_in", dateIn.toString(),
            "date_out", dateOut.toString(),
            "process_path", processPath.toString()
        )
    );
  }

  public static Stream<Arguments> arguments() {
    return Stream.of(
        Arguments.of(
            new Input(
                NETWORK_NODE,
                FBM_WMS_OUTBOUND,
                Map.of(
                    OPTION_DATE_IN_FROM, DATE_IN_FROM.toString(),
                    OPTION_DATE_IN_TO, DATE_IN_TO.toString(),
                    OPTION_VIEW_DATE, VIEW_DATE.toString()
                )
            ),
            new ForecastUseCaseInput(DATE_IN_FROM, DATE_IN_TO),
            new PlanningDistributionGatewayInput(DATE_IN_FROM, DATE_IN_TO, null, null, emptySet())
        ),
        Arguments.of(
            new Input(
                NETWORK_NODE,
                FBM_WMS_OUTBOUND,
                Map.of(
                    OPTION_DATE_OUT_FROM, DATE_OUT_FROM.toString(),
                    OPTION_DATE_OUT_TO, DATE_OUT_TO.toString(),
                    OPTION_VIEW_DATE, VIEW_DATE.toString()
                )
            ),
            new ForecastUseCaseInput(DATE_OUT_FROM, DATE_OUT_TO),
            new PlanningDistributionGatewayInput(null, null, DATE_OUT_FROM, DATE_OUT_TO, emptySet())
        ),
        Arguments.of(
            new Input(
                NETWORK_NODE,
                FBM_WMS_OUTBOUND,
                Map.of(
                    OPTION_DATE_IN_FROM, DATE_IN_FROM.toString(),
                    OPTION_DATE_IN_TO, DATE_IN_TO.toString(),
                    OPTION_DATE_OUT_FROM, DATE_OUT_FROM.toString(),
                    OPTION_DATE_OUT_TO, DATE_OUT_TO.toString(),
                    OPTION_VIEW_DATE, VIEW_DATE.toString(),
                    OPTION_PROCESS_PATHS, "TOT_MONO,NON_TOT_MONO,FAKE_PROCESS_PATH"
                )
            ),
            new ForecastUseCaseInput(DATE_IN_FROM, DATE_OUT_TO),
            new PlanningDistributionGatewayInput(DATE_IN_FROM, DATE_IN_TO, DATE_OUT_FROM, DATE_OUT_TO, Set.of(TOT_MONO, NON_TOT_MONO))
        )
    );
  }

  public static Stream<Arguments> invalidArguments() {
    return Stream.of(
        Arguments.of(
            new Input(
                NETWORK_NODE,
                FBM_WMS_OUTBOUND,
                Map.of(OPTION_VIEW_DATE, VIEW_DATE.toString())
            )
        ),
        Arguments.of(
            new Input(
                NETWORK_NODE,
                FBM_WMS_OUTBOUND,
                Map.of(
                    OPTION_DATE_IN_TO, DATE_OUT_FROM.toString(),
                    OPTION_DATE_OUT_TO, DATE_OUT_TO.toString()
                )
            )
        ),
        Arguments.of(
            new Input(
                NETWORK_NODE,
                FBM_WMS_OUTBOUND,
                Map.of(
                    OPTION_DATE_IN_FROM, DATE_OUT_FROM.toString(),
                    OPTION_DATE_OUT_FROM, DATE_OUT_TO.toString()
                )
            )
        )
    );
  }

  @ParameterizedTest
  @MethodSource("arguments")
  void test(
      final Input input,
      final ForecastUseCaseInput forecastUseCaseInput,
      final PlanningDistributionGatewayInput planningDistributionGatewayInput
  ) {
    // GIVEN
    when(
        getForecastUseCase.execute(
            new GetForecastInput(
                NETWORK_NODE,
                FBM_WMS_OUTBOUND,
                forecastUseCaseInput.dateFrom,
                forecastUseCaseInput.dateTo,
                VIEW_DATE
            )
        )
    ).thenReturn(List.of(1L, 2L));

    when(
        planningDistributionGateway.findByForecastIdsAndDynamicFilters(
            planningDistributionGatewayInput.dateInFrom,
            planningDistributionGatewayInput.dateInTo,
            planningDistributionGatewayInput.dateOutFrom,
            planningDistributionGatewayInput.dateOutTo,
            planningDistributionGatewayInput.processPaths,
            Set.of(1L, 2L)
        )
    ).thenReturn(STORED_PLAN_DISTRIBUTIONS);

    // WHEN
    final var result = adapter.get(input);

    // THEN
    assertEquals(EXPECTED, result.toList());
  }

  @ParameterizedTest
  @MethodSource("invalidArguments")
  void testExceptionCheck(final Input input) {
    // WHEN
    assertThrows(
        InvalidArgumentException.class,
        () -> adapter.get(input)
    );

    // THEN
    verify(getForecastUseCase, never())
        .execute(any());

    verify(planningDistributionGateway, never())
        .findByForecastIdsAndDynamicFilters(any(), any(), any(), any(), any(), any());
  }

  private record ForecastUseCaseInput(Instant dateFrom, Instant dateTo) {

  }

  private record PlanningDistributionGatewayInput(
      Instant dateInFrom,
      Instant dateInTo,
      Instant dateOutFrom,
      Instant dateOutTo,
      Set<ProcessPath> processPaths
  ) {

  }

}
