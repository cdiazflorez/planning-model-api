package com.mercadolibre.planning.model.api.projection.outbound;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.HU_ASSEMBLY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.SALES_DISPATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.projection.outbound.ShippingProjectionTest.throughput;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mockStatic;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.BacklogProjectionService;
import com.mercadolibre.planning.model.api.projection.builder.OutboundProjectionBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class OutboundProjectionUseCaseTest {
  private static final Instant EXECUTION_DATE_FROM = Instant.parse("2023-03-29T00:00:00Z");

  private static final Instant EXECUTION_DATE_TO = Instant.parse("2023-03-29T06:00:00Z");

  private static final OutboundProjectionBuilder PROJECTOR = new OutboundProjectionBuilder();

  private static final Instant[] DATES = {
      EXECUTION_DATE_FROM,
      Instant.parse("2023-03-29T01:00:00Z"),
      Instant.parse("2023-03-29T02:00:00Z"),
      Instant.parse("2023-03-29T03:00:00Z"),
      Instant.parse("2023-03-29T04:00:00Z"),
      Instant.parse("2023-03-29T05:00:00Z"),
      EXECUTION_DATE_TO,
  };

  private static final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> OUTBOUND_BACKLOG = Map.of(
      WAVING, Map.of(
          TOT_MONO, Map.of(DATES[0], 1L),
          NON_TOT_MONO, Map.of(DATES[0], 1L),
          TOT_MULTI_BATCH, Map.of(DATES[0], 1L)
      ),
      PICKING, Map.of(
          TOT_MONO, Map.of(DATES[0], 2500L),
          NON_TOT_MONO, Map.of(DATES[0], 1500L),
          TOT_MULTI_BATCH, Map.of(DATES[0], 2000L)
      ),
      PACKING, Map.of(
          TOT_MONO, Map.of(DATES[0], 3000L),
          NON_TOT_MONO, Map.of(DATES[0], 800L)
      ),
      BATCH_SORTER, Map.of(
          TOT_MULTI_BATCH, Map.of(DATES[0], 1780L)
      ),
      WALL_IN, Map.of(
          TOT_MULTI_BATCH, Map.of(DATES[0], 490L)

      ),
      PACKING_WALL, Map.of(
          TOT_MULTI_BATCH, Map.of(DATES[0], 800L)

      ),
      HU_ASSEMBLY, Map.of(
          TOT_MULTI_BATCH, Map.of(DATES[0], 800L)

      ),
      SALES_DISPATCH, Map.of(
          TOT_MULTI_BATCH, Map.of(DATES[0], 800L)
      )
  );

  private static final Map<ProcessName, Map<Instant, Integer>> OUTBOUND_THROUGHPUT = Map.of(
      PICKING, throughput(3600, 3600, 3600, 3600, 3600, 3600),
      PACKING, throughput(2796, 2796, 2796, 2796, 2796, 2796),
      BATCH_SORTER, throughput(1560, 1560, 1560, 1560, 1560, 1560),
      WALL_IN, throughput(1560, 1560, 1560, 1560, 1560, 1560),
      PACKING_WALL, throughput(1560, 1560, 1560, 1560, 1560, 1560),
      HU_ASSEMBLY, throughput(1560, 1560, 1560, 1560, 1560, 1560),
      SALES_DISPATCH, throughput(1800, 1800, 1800, 1800, 1800, 1800)
  );

  private static final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> FORECAST_BACKLOG = Map.of();

  private static final List<ProcessName> PROCESS_NAMES =
      List.of(WAVING, PICKING, BATCH_SORTER, WALL_IN, PACKING, PACKING_WALL, HU_ASSEMBLY, SALES_DISPATCH);

  private static final Map<Instant, Map<ProcessName, Map<Instant, Integer>>> MOCK_PROJECTION = Map.of();

  private MockedStatic<BacklogProjectionService> wrapper;

  @BeforeEach
  public void setUp() {
    wrapper = mockStatic(BacklogProjectionService.class);
  }

  @AfterEach
  public void tearDown() {
    wrapper.close();
  }

  @Test
  @DisplayName("Test outbound projections use case")
  void check_outbound_projection_test() {

    wrapper.when(() -> BacklogProjectionService.execute(
            EXECUTION_DATE_FROM,
            EXECUTION_DATE_TO,
            OUTBOUND_BACKLOG,
            FORECAST_BACKLOG,
            OUTBOUND_THROUGHPUT,
            PROCESS_NAMES,
            PROJECTOR))
        .thenReturn(MOCK_PROJECTION);

    OutboundProjectionUseCase.execute(
        EXECUTION_DATE_FROM,
        EXECUTION_DATE_TO,
        OUTBOUND_BACKLOG,
        emptyMap(),
        OUTBOUND_THROUGHPUT
    );

    wrapper.verify(
        () -> BacklogProjectionService.execute(
            eq(EXECUTION_DATE_FROM),
            eq(EXECUTION_DATE_TO),
            eq(OUTBOUND_BACKLOG),
            eq(FORECAST_BACKLOG),
            eq(OUTBOUND_THROUGHPUT),
            eq(PROCESS_NAMES),
            refEq(PROJECTOR)
        )
    );

  }
}
