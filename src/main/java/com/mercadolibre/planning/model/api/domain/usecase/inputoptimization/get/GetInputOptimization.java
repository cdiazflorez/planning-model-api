package com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.get;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class GetInputOptimization {

    String warehouseId;

    Map<DomainType, Map<String, List<Object>>> domains;

}
