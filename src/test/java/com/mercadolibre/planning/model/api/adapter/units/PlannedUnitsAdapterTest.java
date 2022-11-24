package com.mercadolibre.planning.model.api.adapter.units;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastIds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionDynamicRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistribution;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlannedUnitsAdapterTest {

  private static final Instant DATE_IN_1 = A_DATE_UTC.toInstant();

  private static final Instant DATE_IN_2 = A_DATE_UTC.plusHours(1).toInstant();

  private static final Instant DATE_IN_3 = A_DATE_UTC.plusHours(2).toInstant();

  private static final Instant DATE_OUT_1 = A_DATE_UTC.plusHours(6).toInstant();

  private static final Instant DATE_OUT_2 = A_DATE_UTC.plusHours(7).toInstant();

  private static final Instant DATE_OUT_3 = A_DATE_UTC.plusHours(8).toInstant();

  private static final Set<Grouper> GROUPERS = Set.of(Grouper.DATE_IN, Grouper.DATE_OUT, Grouper.PROCESS_PATH);

  private static final Set<ProcessPath> PROCESS_PATHS = Set.of(TOT_MONO, NON_TOT_MONO);

  @Mock
  private GetForecastUseCase getForecastUseCase;

  @Mock
  private PlanningDistributionDynamicRepository repository;

  @InjectMocks
  private PlannedUnitsAdapter adapter;

  @Test
  @DisplayName("when searching units from only one forecast the units should be returned immediately")
  void testGetPlannedUnitsFromOneForecast() {
    // GIVEN
    when(getForecastUseCase.execute(new GetForecastInput(
            WAREHOUSE_ID,
            FBM_WMS_OUTBOUND,
            DATE_IN_1,
            DATE_OUT_3,
            A_DATE_UTC.toInstant()
        ))
    ).thenReturn(mockForecastIds());

    final var distributions = List.of(
        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, TOT_MONO, 33.5),
        new PlanningDistribution(1L, DATE_IN_2, DATE_OUT_3, TOT_MONO, 44.5),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, TOT_MONO, 30.0),

        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, NON_TOT_MONO, 0),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, 10)
    );

    when(repository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        DATE_IN_1,
        DATE_IN_3,
        DATE_OUT_1,
        DATE_OUT_3,
        PROCESS_PATHS,
        GROUPERS,
        new HashSet<>(mockForecastIds())
    )).thenReturn(distributions);

    // WHEN
    final var result = adapter.getPlanningDistributions(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        PROCESS_PATHS,
        DATE_IN_1,
        DATE_IN_3,
        DATE_OUT_1,
        DATE_OUT_3,
        GROUPERS,
        A_DATE_UTC.toInstant()
    );

    // THEN
    assertEquals(distributions, result);
  }

  @Test
  @DisplayName("when searching units from multiple forecasts then overlapping date_ins belong to the latest forecast")
  void testGetPlannedUnitsFromMultipleForecast() {
    // GIVEN
    when(getForecastUseCase.execute(new GetForecastInput(
            WAREHOUSE_ID,
            FBM_WMS_OUTBOUND,
            DATE_IN_1,
            DATE_OUT_3,
            A_DATE_UTC.toInstant()
        ))
    ).thenReturn(mockForecastIds());

    final var distributions = List.of(
        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, TOT_MONO, 33.5),
        new PlanningDistribution(1L, DATE_IN_2, DATE_OUT_3, TOT_MONO, 44.5),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, TOT_MONO, 30.0),
        new PlanningDistribution(2L, DATE_IN_2, DATE_OUT_3, TOT_MONO, 55.5),
        new PlanningDistribution(2L, DATE_IN_3, DATE_OUT_2, TOT_MONO, 66.6),

        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, NON_TOT_MONO, 0),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, 10),
        new PlanningDistribution(2L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, 37.2)
    );

    when(repository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        DATE_IN_1,
        DATE_IN_3,
        DATE_OUT_1,
        DATE_OUT_3,
        PROCESS_PATHS,
        GROUPERS,
        new HashSet<>(mockForecastIds())
    )).thenReturn(distributions);

    // WHEN
    final var result = adapter.getPlanningDistributions(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        PROCESS_PATHS,
        DATE_IN_1,
        DATE_IN_3,
        DATE_OUT_1,
        DATE_OUT_3,
        GROUPERS,
        A_DATE_UTC.toInstant()
    );

    // THEN
    final var expected = List.of(
        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, TOT_MONO, 33.5),
        new PlanningDistribution(2L, DATE_IN_2, DATE_OUT_3, TOT_MONO, 55.5),
        new PlanningDistribution(2L, DATE_IN_3, DATE_OUT_2, TOT_MONO, 66.6),
        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, NON_TOT_MONO, 0),
        new PlanningDistribution(2L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, 37.2)
    );

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("when searching units from only one forecast the units should be returned immediately without date in")
  void testGetPlannedUnitsFromOneForecastWirhOutDateIn() {
    // GIVEN
    when(getForecastUseCase.execute(new GetForecastInput(
            WAREHOUSE_ID,
            FBM_WMS_OUTBOUND,
            DATE_OUT_1,
            DATE_OUT_3,
            A_DATE_UTC.toInstant()
        ))
    ).thenReturn(mockForecastIds());

    final var distributions = List.of(
        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, TOT_MONO, 33.5),
        new PlanningDistribution(1L, DATE_IN_2, DATE_OUT_3, TOT_MONO, 44.5),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, TOT_MONO, 30.0),

        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, NON_TOT_MONO, 0),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, 10)
    );

    when(repository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        null,
        null,
        DATE_OUT_1,
        DATE_OUT_3,
        PROCESS_PATHS,
        GROUPERS,
        new HashSet<>(mockForecastIds())
    )).thenReturn(distributions);

    // WHEN
    final var result = adapter.getPlanningDistributions(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        PROCESS_PATHS,
        null,
        null,
        DATE_OUT_1,
        DATE_OUT_3,
        GROUPERS,
        A_DATE_UTC.toInstant()
    );

    // THEN
    assertEquals(distributions, result);
  }

}
