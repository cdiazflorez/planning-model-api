package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CptProjectionRequest {

  @NotNull
  private String warehouseId;

  private ProjectionType type;

  @NotNull
  private List<ProcessName> processName;

  @NotNull
  private ZonedDateTime dateFrom;

  @NotNull
  private ZonedDateTime dateTo;

  private List<QuantityByDate> backlog;

  @NotNull
  private String timeZone;

  private boolean applyDeviation;

  private List<Simulation> simulations;

  private ZonedDateTime viewDate;

}
