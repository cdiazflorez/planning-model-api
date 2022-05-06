package com.mercadolibre.planning.model.api.web.controller.adapter;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionByArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper.UnitsAreaDistributionMapper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogByArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.CurrentBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.PlannedBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.QuantityAtArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.ProjectionResult;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.SimpleProcessedBacklog;
import com.mercadolibre.planning.model.api.web.controller.projection.BacklogProjectionAdapter;
import com.mercadolibre.planning.model.api.web.controller.projection.request.AreaShareAtSlaAndProcessDto;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ThroughputDto;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BacklogProjectionAdapterTest {

  private static final String NO_AREA = "undefined";

  private static final Instant DATE_FROM = Instant.parse("2020-07-27T09:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2020-07-28T08:00:00Z");

  private static final Instant OP_HOUR_A = Instant.parse("2022-03-31T12:00:00Z");

  private static final Instant OP_HOUR_B = Instant.parse("2022-03-31T13:00:00Z");

  @InjectMocks
  private BacklogProjectionAdapter useCase;

  @Mock
  private BacklogProjectionByArea calculateBacklogProjectionByArea;

  @Test
  public void testBacklogProjectionByArea() {
    // GIVEN
    when(
        calculateBacklogProjectionByArea.execute(
            eq(DATE_FROM),
            eq(DATE_TO),
            eq(FBM_WMS_OUTBOUND),
            eq(List.of(WAVING, PICKING, PACKING)),
            anyMap(),
            any(PlannedBacklogBySla.class),
            anyMap(),
            any(UnitsAreaDistributionMapper.class)
        )
    ).thenReturn(projectionResults());

    // WHEN
    final var result = useCase.projectionByArea(
        DATE_FROM,
        DATE_TO,
        FBM_WMS_OUTBOUND,
        List.of(WAVING, PICKING, PACKING),
        getThroughput(),
        getPlanningUnits(),
        getCurrentBacklogBySla(),
        getAreaDistributions()
    );

    // THEN
    assertNotNull(result);
  }

  private List<ThroughputDto> getThroughput() {
    return List.of(
        new ThroughputDto(FBM_WMS_OUTBOUND, DATE_FROM, WAVING, 100),
        new ThroughputDto(FBM_WMS_OUTBOUND, DATE_FROM, PICKING, 150)
    );
  }

  private List<GetPlanningDistributionOutput> getPlanningUnits() {
    final ZonedDateTime dateIn = ZonedDateTime.ofInstant(DATE_FROM, ZoneOffset.UTC);
    final ZonedDateTime dateOutA = ZonedDateTime.ofInstant(Instant.parse("2020-07-27T18:00:00Z"), ZoneOffset.UTC);
    final ZonedDateTime dateOutB = ZonedDateTime.ofInstant(Instant.parse("2020-07-27T19:00:00Z"), ZoneOffset.UTC);

    return List.of(
        new GetPlanningDistributionOutput(dateIn, dateOutA, UNITS, 150, false),
        new GetPlanningDistributionOutput(dateIn, dateOutB, UNITS, 130, false)
    );
  }

  private List<CurrentBacklogBySla> getCurrentBacklogBySla() {
    return List.of(
        new CurrentBacklogBySla(WAVING, DATE_FROM, 1500),
        new CurrentBacklogBySla(PICKING, DATE_FROM, 1000),
        new CurrentBacklogBySla(PACKING, DATE_FROM, 500)
    );
  }

  private List<AreaShareAtSlaAndProcessDto> getAreaDistributions() {
    return List.of(
        new AreaShareAtSlaAndProcessDto(PICKING, DATE_FROM, "RK-H", 0.5),
        new AreaShareAtSlaAndProcessDto(PICKING, DATE_FROM, "RK-L", 0.5)
    );
  }

  private Map<ProcessName, List<ProjectionResult<BacklogByArea>>> projectionResults() {

    final List<ProjectionResult<BacklogByArea>> wavingResults = List.of(
        new ProjectionResult<>(
            OP_HOUR_A,
            new SimpleProcessedBacklog<>(
                new BacklogByArea(List.of(
                    new QuantityAtArea(NO_AREA, 70)
                )),
                new BacklogByArea(List.of(
                    new QuantityAtArea(NO_AREA, 0)
                ))
            )
        ),
        new ProjectionResult<>(
            OP_HOUR_B,
            new SimpleProcessedBacklog<>(
                new BacklogByArea(List.of(
                    new QuantityAtArea(NO_AREA, 130)
                )),
                new BacklogByArea(List.of(
                    new QuantityAtArea(NO_AREA, 0)
                ))
            )
        )
    );

    final List<ProjectionResult<BacklogByArea>> pickingResults = List.of(
        new ProjectionResult<>(
            OP_HOUR_A,
            new SimpleProcessedBacklog<>(
                new BacklogByArea(List.of(
                    new QuantityAtArea("RK-L", 100),
                    new QuantityAtArea("RK-H", 0)
                )),
                new BacklogByArea(List.of(
                    new QuantityAtArea("RK-H", 45),
                    new QuantityAtArea("RK-L", 110)
                ))
            )
        ),
        new ProjectionResult<>(
            OP_HOUR_B,
            new SimpleProcessedBacklog<>(
                new BacklogByArea(List.of(
                    new QuantityAtArea("RK-H", 45),
                    new QuantityAtArea("RK-L", 155)
                )),
                new BacklogByArea(List.of(
                    new QuantityAtArea("RK-H", 0),
                    new QuantityAtArea("RK-L", 85)
                ))
            )
        )
    );

    final List<ProjectionResult<BacklogByArea>> packingResults = List.of(
        new ProjectionResult<>(
            OP_HOUR_A,
            new SimpleProcessedBacklog<>(
                new BacklogByArea(List.of(
                    new QuantityAtArea(NO_AREA, 100)
                )),
                new BacklogByArea(List.of(
                    new QuantityAtArea(NO_AREA, 0)
                ))
            )
        ),
        new ProjectionResult<>(
            OP_HOUR_B,
            new SimpleProcessedBacklog<>(
                new BacklogByArea(List.of(
                    new QuantityAtArea(NO_AREA, 45)
                )),
                new BacklogByArea(List.of(
                    new QuantityAtArea(NO_AREA, 0)
                ))
            )
        )
    );

    return Map.of(
        WAVING, wavingResults,
        PICKING, pickingResults,
        PACKING, packingResults
    );
  }
}
