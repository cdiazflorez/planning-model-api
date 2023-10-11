package com.mercadolibre.planning.model.api.web.controller.projection.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import java.time.ZonedDateTime;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuantityByDate {

  @NotNull
  private ZonedDateTime date;

  private Double quantity;

  private Map<ProcessPath, Double> processPath;

  public Backlog toBacklog() {
    return new Backlog(date, quantity.intValue());
  }
}
