package com.mercadolibre.planning.model.api.web.controller.availablecapacity;

import com.mercadolibre.planning.model.api.projection.availablecapacity.AvailableCapacity;
import com.mercadolibre.planning.model.api.projection.availablecapacity.AvailableCapacityUseCase;
import com.mercadolibre.planning.model.api.web.controller.availablecapacity.request.Request;
import com.newrelic.api.agent.Trace;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterID}/projections/capacity")
public class AvailableCapacityController {

  private AvailableCapacityUseCase availableCapacityUseCase;

  @Trace(dispatcher = true)
  @PostMapping
  public ResponseEntity<AvailableCapacity> getAvailableCapacity(
      @PathVariable final String logisticCenterID,
      @RequestBody @Valid final Request request
  ) {
    return ResponseEntity.ok(availableCapacityUseCase.execute(
        request.getExecutionDateFrom(),
        request.getExecutionDateTo(),
        request.asCurrentBacklog(),
        request.asForecastBacklog(),
        request.getThroughput(),
        request.getCycleTimeBySla()
        ));
  }
}
