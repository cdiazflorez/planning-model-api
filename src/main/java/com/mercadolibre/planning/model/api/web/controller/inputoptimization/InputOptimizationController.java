package com.mercadolibre.planning.model.api.web.controller.inputoptimization;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.InputOptimizationService;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.get.GetInputOptimization;
import com.mercadolibre.planning.model.api.web.controller.inputoptimization.request.InputOptimizationRequest;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/inputs")
public class InputOptimizationController {

    private InputOptimizationService inputOptimizationService;

    @PostMapping
    public ResponseEntity<Map<DomainType, Object>> getInputsOptimization(@Valid @RequestBody final InputOptimizationRequest request) {

        final Map<DomainType, Map<String, List<Object>>> domainsWithFilters = request.getDomains() == null || request.getDomains().isEmpty()
        ? Map.of()
        : request.getDomains();

        domainsWithFilters.keySet().forEach(
                key -> {
                    if (domainsWithFilters.get(key) == null || domainsWithFilters.get(key).isEmpty()) {
                        domainsWithFilters.put(key, Map.of());
                    }
                }
        );

        final GetInputOptimization getInputOptimization = new GetInputOptimization(request.getWarehouseId(), domainsWithFilters);

        return ResponseEntity.ok(inputOptimizationService.getInputOptimization(getInputOptimization));
    }


}
