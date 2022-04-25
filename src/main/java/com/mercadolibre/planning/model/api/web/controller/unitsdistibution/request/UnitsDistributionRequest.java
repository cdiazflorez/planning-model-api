package com.mercadolibre.planning.model.api.web.controller.unitsdistibution.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.ZonedDateTime;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Class that receives the parameters of units-distribution. */
@AllArgsConstructor
@Getter
@Setter
public class UnitsDistributionRequest {

  @NotBlank
  private String logisticCenterId;

  @NotBlank
  private ZonedDateTime date;

  @NotBlank
  private ProcessName processName;

  @NotBlank
  private String area;

  @NotBlank
  private Double quantity;

  @NotBlank
  private String quantityMetricUnit;
}
