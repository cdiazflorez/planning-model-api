package com.mercadolibre.planning.model.api.web.controller.inputcatalog.request;

import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
import lombok.Value;

@Value
public class InputsCatalogRequest {

    @NotEmpty
    String warehouseId;

    Map<InputId, Map<String, List<Object>>> inputs;

}
