package com.mercadolibre.planning.model.api.domain.usecase.sla;

import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;

import java.util.List;

public interface GetSlaByWarehouseService {
    List<GetSlaByWarehouseOutput> execute(final GetSlaByWarehouseInput input);
}
