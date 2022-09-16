package com.mercadolibre.planning.model.api.projection.dto;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Builder
@Data
@Value
public class ProjectionRequest {
  Instant dateFrom;

  Instant dateTo;

  Map<ProcessName, Map<Instant, Integer>> throughputByProcess;

  Map<ProcessName, Map<Instant, Integer>> backlogBySlaAndProcess;

  List<PlanningDistribution> forecastSales;

  Map<Instant, PackingRatio> ratioByHour;

  @Value
  public static class PlanningDistribution {
    ZonedDateTime dateIn;

    ZonedDateTime dateOut;

    long total;
  }

  @Value
  public static class PackingRatio {
    Double packingToteRatio;

    Double packingWallRatio;
  }
}
