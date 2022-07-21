package com.mercadolibre.planning.model.api.web.controller.inputoptimization;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.InputOptimizationService;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.request.InputOptimizationRequest;
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

        return ResponseEntity.ok(inputOptimizationService.getInputOptimization(request));

    }


}
