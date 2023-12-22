package com.mercadolibre.planning.model.api.projection.outbound;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.HU_ASSEMBLY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.SALES_DISPATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.BacklogProjectionService;
import com.mercadolibre.planning.model.api.projection.builder.OutboundProjectionBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class OutboundProjectionUseCase {

  private static final List<ProcessName> PROCESSES =
      List.of(WAVING, PICKING, BATCH_SORTER, WALL_IN, PACKING, PACKING_WALL, HU_ASSEMBLY, SALES_DISPATCH);

  private OutboundProjectionUseCase() {
  }

  /**
   * Calls the {@link BacklogProjectionService} to obtain the backlog projection by process,
   * using the {@link OutboundProjectionBuilder} class that considers all processes from Waving to Shipping.
   *
   * @param executionDateFrom The start time of the projection.
   * @param executionDateTo   The end time of the projection.
   * @param currentBacklog    The actual backlog in the Fulfillment Center (FC).
   * @param forecastBacklog   The planned backlog expected to be generated in the future.
   * @param throughput        The processing capacity per hour, grouped by operation time and process.
   * @return A Map representing the backlog grouped by CPT, Process, and Operation time.
   */
  public static Map<Instant, Map<ProcessName, Map<Instant, Integer>>> execute(
      final Instant executionDateFrom,
      final Instant executionDateTo,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput
  ) {
    final OutboundProjectionBuilder projector = new OutboundProjectionBuilder();
    return BacklogProjectionService.execute(executionDateFrom, executionDateTo, currentBacklog, forecastBacklog, throughput, PROCESSES,
        projector);

  }
}
