package com.mercadolibre.planning.model.api.web.controller.inputcatalog;

import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.InputCatalogService;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.get.GetInputOptimization;
import com.mercadolibre.planning.model.api.web.controller.inputcatalog.request.InputsCatalogRequest;
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
public class InputsCatalogController {

    private InputCatalogService inputCatalogService;

    @PostMapping
    public ResponseEntity<Map<InputId, Object>> getInputsOptimization(@Valid @RequestBody final InputsCatalogRequest request) {

        final Map<InputId, Map<String, List<Object>>> domainsWithFilters = request.getInputs() == null || request.getInputs().isEmpty()
        ? Map.of()
        : request.getInputs();

        domainsWithFilters.keySet().forEach(
                key -> {
                    if (domainsWithFilters.get(key) == null || domainsWithFilters.get(key).isEmpty()) {
                        domainsWithFilters.put(key, Map.of());
                    }
                }
        );

        final GetInputOptimization getInputOptimization = new GetInputOptimization(request.getWarehouseId(), domainsWithFilters);

        return ResponseEntity.ok(inputCatalogService.getInputOptimization(getInputOptimization));
    }


}
