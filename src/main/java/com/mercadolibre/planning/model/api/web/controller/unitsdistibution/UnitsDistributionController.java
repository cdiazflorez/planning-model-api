package com.mercadolibre.planning.model.api.web.controller.unitsdistibution;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.metrics.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.GetUnitsInput;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.UnitsDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.UnitsInput;
import com.mercadolibre.planning.model.api.web.controller.unitsdistibution.request.UnitsDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.unitsdistibution.response.GetUnitsDistributionResponse;
import com.mercadolibre.planning.model.api.web.controller.unitsdistibution.response.UnitsDistributionResponse;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller to save and query units-distribution. */
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/units_distribution")
public class UnitsDistributionController {

  private final UnitsDistributionService unitsDistributionService;

  @PostMapping
  public ResponseEntity<UnitsDistributionResponse> save(@RequestBody List<UnitsDistributionRequest> unitsDistributionRequests,
                                                        @PathVariable final Workflow workflow) {

    List<UnitsDistribution> result = unitsDistributionService.save(toUnitsInput(unitsDistributionRequests, workflow));

    if (!result.isEmpty()) {
      return ResponseEntity.ok(new UnitsDistributionResponse("Successful", result.size()));
    }
    return ResponseEntity.ok(new UnitsDistributionResponse("Error", 0));
  }

  private List<UnitsInput> toUnitsInput(List<UnitsDistributionRequest> unitsDistributionRequests, Workflow workflow) {
    return unitsDistributionRequests.stream().map(
        unitsDistributionRequest -> new UnitsInput(unitsDistributionRequest.getLogisticCenterId(), unitsDistributionRequest.getDate(),
            unitsDistributionRequest.getProcessName(), unitsDistributionRequest.getArea(), unitsDistributionRequest.getQuantity(),
            unitsDistributionRequest.getQuantityMetricUnit(), workflow)).collect(Collectors.toList());
  }

  @GetMapping
  public ResponseEntity<List<GetUnitsDistributionResponse>> get(
      @PathVariable final Workflow workflow,
      @RequestParam @NonNull String warehouseId,
      @RequestParam @DateTimeFormat(iso = DATE_TIME) @NonNull final ZonedDateTime dateFrom,
      @RequestParam @DateTimeFormat(iso = DATE_TIME) @NonNull final ZonedDateTime dateTo) {

    List<UnitsDistribution> getUnitsDistribution =
        unitsDistributionService.get(new GetUnitsInput(dateFrom, dateTo, warehouseId));

    return ResponseEntity.ok(getUnitsDistribution.stream().map(
        unitsDistribution -> new GetUnitsDistributionResponse(unitsDistribution.getLogisticCenterId(), unitsDistribution.getDate(),
            unitsDistribution.getProcessName(), unitsDistribution.getArea(), unitsDistribution.getQuantity(),
            unitsDistribution.getQuantityMetricUnit())).collect(Collectors.toList()));

  }
}
