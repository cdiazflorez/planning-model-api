package com.mercadolibre.planning.model.api.domain.usecase.forecast.update;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

public record UpdateStaffingPlanInput(
    String logisticCenterId,
    Workflow workflow,
    long userId,
    List<Resource> resources
) {
  public record Resource(EntityType name, List<ResourceValues> values) {
  }

  public record ResourceValues(
      @NotNull
      double value,
      @NotNull
      ZonedDateTime date,
      Map<String, String> tags
  ) {
  }
}
