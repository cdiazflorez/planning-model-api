package com.mercadolibre.planning.model.api.web.controller.planningdistribution;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import java.time.Instant;
import java.time.ZonedDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetPlanningDistributionRequest {

  @NotBlank
  private String warehouseId;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private ZonedDateTime dateOutFrom;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private ZonedDateTime dateOutTo;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private ZonedDateTime dateInFrom;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private ZonedDateTime dateInTo;

  private boolean applyDeviation;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private Instant viewDate;

  private Workflow workflow;

  public GetPlanningDistributionInput toGetPlanningDistInput(final Workflow workflow) {
    return GetPlanningDistributionInput.builder()
        .warehouseId(warehouseId)
        .workflow(workflow)
        .dateOutFrom(dateOutFrom)
        .dateOutTo(dateOutTo)
        .dateInFrom(dateInFrom)
        .dateInTo(dateInTo)
        .applyDeviation(applyDeviation)
        .viewDate(viewDate)
        .build();
  }
}
