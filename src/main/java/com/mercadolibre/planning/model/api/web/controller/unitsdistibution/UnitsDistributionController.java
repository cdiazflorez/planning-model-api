package com.mercadolibre.planning.model.api.web.controller.unitsdistibution;

import com.mercadolibre.planning.model.api.domain.entity.forecast.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.CreateUnitsDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.GetUnitsDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create.UnitsInput;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.get.GetUnitsInput;
import com.mercadolibre.planning.model.api.web.controller.unitsdistibution.request.UnitsDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.unitsdistibution.response.GetUnitsDistributionResponse;
import com.mercadolibre.planning.model.api.web.controller.unitsdistibution.response.UnitsDistributionResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/units_distribution")
public class UnitsDistributionController {

    private final CreateUnitsDistributionService createUnitsDistributionService;
    private final GetUnitsDistributionService getUnitsDistributionService;

    @PostMapping("/save")
    public ResponseEntity<UnitsDistributionResponse> save(@RequestBody List<UnitsDistributionRequest> unitsDistributionRequests){

        List<UnitsDistribution> result = createUnitsDistributionService.save(toUnitsInput(unitsDistributionRequests));

        if(!result.isEmpty()){
            return ResponseEntity.ok(new UnitsDistributionResponse("Successful", result.size()));
        }
        return ResponseEntity.ok(new UnitsDistributionResponse("Error", 0));
    }

    private List<UnitsInput> toUnitsInput(List<UnitsDistributionRequest> unitsDistributionRequests){
        return unitsDistributionRequests.stream().map(unitsDistributionRequest -> UnitsInput.builder()
                .area(unitsDistributionRequest.getArea())
                .processName(unitsDistributionRequest.getProcessName())
                .date(unitsDistributionRequest.getDate())
                .quantity(unitsDistributionRequest.getQuantity())
                .quantityMetricUnit(unitsDistributionRequest.getQuantityMetricUnit())
                .logisticCenterId(unitsDistributionRequest.getLogisticCenterId()).build()).collect(Collectors.toList());
    }

    @GetMapping( "/data")
    public ResponseEntity<List<GetUnitsDistributionResponse>> get(
            @RequestParam @NonNull String warehouseId,
            @RequestParam @DateTimeFormat(iso = DATE_TIME) @NonNull final ZonedDateTime dateFrom,
            @RequestParam @DateTimeFormat(iso = DATE_TIME) @NonNull final ZonedDateTime dateTo) {

        List<UnitsDistribution> getUnitsDistribution = getUnitsDistributionService.get(GetUnitsInput.builder().dateFrom(dateFrom).dateTo(dateTo).wareHouseId(warehouseId).build());

        return ResponseEntity.ok(getUnitsDistribution.stream().map( unitsDistribution -> GetUnitsDistributionResponse.builder()
                .area(unitsDistribution.getArea())
                .date(unitsDistribution.getDate())
                .id(unitsDistribution.getId())
                .logisticCenterId(unitsDistribution.getLogisticCenterId())
                .quantityMetricUnit(unitsDistribution.getQuantityMetricUnit())
                .quantity(unitsDistribution.getQuantity())
                .build())
                .collect(Collectors.toList()));

    }
}
