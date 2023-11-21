package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import java.time.ZonedDateTime;
import java.util.List;

public final class StaffingPlanTestUtils {

  private static final ZonedDateTime DATE_FROM = A_DATE_UTC;

  private StaffingPlanTestUtils() {
  }

  public static List<EntityOutput> mockHeadcount() {
    return List.of(
        entityOutput(DATE_FROM, 25, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(1), 40, TOT_MONO, FORECAST, false),
        entityOutput(DATE_FROM.plusHours(1), 50, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(2), 75, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(3), 100, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(4), 125, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(5), 150, TOT_MONO, SIMULATION, false),
        entityOutput(DATE_FROM, 13, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(1), 26, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(2), 39, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(3), 52, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(3), 55, TOT_MULTI_ORDER, FORECAST, false),
        entityOutput(DATE_FROM.plusHours(4), 65, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(5), 78, TOT_MULTI_ORDER, SIMULATION, false),
        entityOutput(DATE_FROM, 10, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(1), 20, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(2), 30, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(2), 33, TOT_MULTI_BATCH, FORECAST, false),
        entityOutput(DATE_FROM.plusHours(3), 40, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(4), 50, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(5), 60, TOT_MULTI_BATCH, SIMULATION, false),
        entityOutput(DATE_FROM.plusHours(5), 60, TOT_MULTI_BATCH, SIMULATION, false)
    );
  }

  public static List<EntityOutput> mockMaxCapacity() {
    return List.of(
       maxCapacityOutput(DATE_FROM, 25, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(1), 40, FORECAST),
       maxCapacityOutput(DATE_FROM.plusHours(1), 50, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(2), 75, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(3), 100, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(4), 125, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(5), 150, SIMULATION),
       maxCapacityOutput(DATE_FROM, 13, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(1), 26, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(2), 39, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(3), 52, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(3), 55, FORECAST),
       maxCapacityOutput(DATE_FROM.plusHours(4), 65, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(5), 78, SIMULATION),
       maxCapacityOutput(DATE_FROM, 10, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(1), 20, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(2), 30, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(2), 33, FORECAST),
       maxCapacityOutput(DATE_FROM.plusHours(3), 40, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(4), 50, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(5), 60, SIMULATION),
       maxCapacityOutput(DATE_FROM.plusHours(5), 60, SIMULATION)
    );
  }

  public static List<ProductivityOutput> mockProductivity() {

    return List.of(
        productivityOutput(DATE_FROM, 25, TOT_MONO, SIMULATION, 1),
        productivityOutput(DATE_FROM, 20, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM, 35, TOT_MONO, FORECAST, 2),
        productivityOutput(DATE_FROM.plusHours(1), 50, TOT_MONO, SIMULATION, 1),
        productivityOutput(DATE_FROM.plusHours(1), 63, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(2), 75, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(3), 100, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(4), 125, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(5), 150, TOT_MONO, FORECAST, 1),
        productivityOutput(DATE_FROM, 13, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(1), 26, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(1), 36, TOT_MULTI_ORDER, SIMULATION, 1),
        productivityOutput(DATE_FROM.plusHours(2), 39, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(3), 52, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(4), 65, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(4), 68, TOT_MULTI_ORDER, SIMULATION, 1),
        productivityOutput(DATE_FROM.plusHours(5), 78, TOT_MULTI_ORDER, FORECAST, 1),
        productivityOutput(DATE_FROM.plusHours(5), 78, TOT_MULTI_ORDER, FORECAST, 1)
    );
  }

  public static List<EntityOutput> mockThroughputs() {
    return List.of(
        entityOutput(DATE_FROM, 25, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(1), 50, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(2), 75, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(3), 100, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(4), 125, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(5), 150, TOT_MONO, SIMULATION, true),
        entityOutput(DATE_FROM, 13, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(1), 26, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(2), 39, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(3), 52, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(4), 65, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(5), 78, TOT_MULTI_ORDER, SIMULATION, true),
        entityOutput(DATE_FROM, 10, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(1), 20, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(2), 30, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(3), 40, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(4), 50, TOT_MULTI_BATCH, SIMULATION, true),
        entityOutput(DATE_FROM.plusHours(5), 60, TOT_MULTI_BATCH, SIMULATION, true)
    );
  }

  private static ProductivityOutput productivityOutput(
      final ZonedDateTime date,
      final double quantity,
      final ProcessPath processPath,
      final Source source,
      final int abilityLevel
  ) {
    return ProductivityOutput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .processName(PICKING)
        .processPath(processPath)
        .date(date)
        .metricUnit(UNITS_PER_HOUR)
        .source(source)
        .value(quantity)
        .abilityLevel(abilityLevel)
        .build();
  }

  private static EntityOutput entityOutput(
      final ZonedDateTime date,
      final long quantity,
      final ProcessPath processPath,
      final Source source,
      final boolean isThroughput
  ) {
    return EntityOutput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .processName(PICKING)
        .processPath(processPath)
        .date(date)
        .type(isThroughput ? THROUGHPUT : EFFECTIVE_WORKERS)
        .metricUnit(isThroughput ? UNITS_PER_HOUR : WORKERS)
        .source(source)
        .value(quantity)
        .build();
  }

  private static EntityOutput maxCapacityOutput(
      final ZonedDateTime date,
      final long quantity,
      final Source source
  ) {
    return EntityOutput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .date(date)
        .type(MAX_CAPACITY)
        .metricUnit(WORKERS)
        .source(source)
        .value(quantity)
        .build();
  }
}
