package com.mercadolibre.planning.model.api.web.controller.configuration.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;

@Value
public class CycleTimeRequest {
  @NotNull
  Set<Workflow> workflows;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  Instant dateFrom;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  Instant dateTo;

  @NotNull
  List<Instant> slas;

  @NotBlank
  String timeZone;
}
