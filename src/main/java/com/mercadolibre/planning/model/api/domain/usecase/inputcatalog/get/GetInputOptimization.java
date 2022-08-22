package com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.get;

import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import java.util.List;
import java.util.Map;
import lombok.Value;

@Value
public class GetInputOptimization {

    String warehouseId;

    Map<InputId, Map<String, List<Object>>> domains;

}
