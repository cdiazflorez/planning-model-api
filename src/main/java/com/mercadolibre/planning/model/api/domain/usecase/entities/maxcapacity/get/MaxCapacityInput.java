package com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Value;

@Value
public class MaxCapacityInput {

  String warehouseId;

  Workflow workflow;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  List<Simulation> simulations;
}
