package com.mercadolibre.planning.model.api.projection.dto.request.total;

import com.mercadolibre.planning.model.api.exception.BadRequestException;
import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class BacklogProjectionTotalRequest {
  @NotNull
  Instant dateFrom;
  @NotNull
  Instant dateTo;
  @NotNull
  BacklogRequest backlog;
  @NotNull
  BacklogRequest plannedUnit;
  @NotNull
  List<Throughput> throughput;

  public void validateDateRange() {
    if (dateFrom.isAfter(dateTo)) {
      throw new BadRequestException("date_from is after date_to");
    }
  }
}
