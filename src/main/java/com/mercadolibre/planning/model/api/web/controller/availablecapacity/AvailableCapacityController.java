package com.mercadolibre.planning.model.api.web.controller.availablecapacity;

import com.mercadolibre.planning.model.api.projection.availablecapacity.AvailableCapacity;
import com.mercadolibre.planning.model.api.projection.availablecapacity.AvailableCapacityUseCase;
import com.mercadolibre.planning.model.api.projection.availablecapacity.CapacityBySLA;
import com.mercadolibre.planning.model.api.web.controller.availablecapacity.request.Request;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
        final List<CapacityBySLA> capacities = availableCapacityUseCase.execute(
                request.getExecutionDateFrom(),
                request.getExecutionDateTo(),
                request.asCurrentBacklog(),
                request.asForecastBacklog(),
                request.getThroughput(),
                request.getCycleTimeBySla()
        );
        return ResponseEntity.ok(new AvailableCapacity(capacities));
    }
}
