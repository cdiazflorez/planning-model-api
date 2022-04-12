package com.mercadolibre.planning.model.api.web.controller.unitsdistibution;

import com.mercadolibre.planning.model.api.domain.entity.forecast.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.CreateUnitsDistributionService;
import com.mercadolibre.planning.model.api.web.controller.unitsdistibution.request.UnitsDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.unitsdistibution.response.UnitsDistributionResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/units_distribution")
public class UnitsDistributionController {

    private final CreateUnitsDistributionService createUnitsDistributionService;

    @PostMapping("/save")
    public ResponseEntity<UnitsDistributionResponse> save(@RequestBody UnitsDistributionRequest unitsDistributionRequest){

        List<UnitsDistribution> result = createUnitsDistributionService.save(unitsDistributionRequest.toUnitsDistributionInput());

        if(!result.isEmpty()){
            return ResponseEntity.ok(new UnitsDistributionResponse("Successful", result.size()));
        }
        return ResponseEntity.ok(new UnitsDistributionResponse("Error", 0));
    }
}
