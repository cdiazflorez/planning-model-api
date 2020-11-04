package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.QuantityByDate;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

import java.util.List;

@Data
@AllArgsConstructor
public class SimulationEntity {

    @NotNull
    private EntityType type;

    @NotNull
    private List<QuantityByDate> values;
}
