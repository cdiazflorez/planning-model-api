package com.mercadolibre.planning.model.api.web.controller.inputoptimization.request;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
import lombok.Value;

@Value
public class InputOptimizationRequest {

    @NotEmpty
    String warehouseId;

    Map<DomainType, Map<String, List<Object>>> inputs;

}
