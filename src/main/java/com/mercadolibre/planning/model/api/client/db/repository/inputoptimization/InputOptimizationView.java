package com.mercadolibre.planning.model.api.client.db.repository.inputoptimization;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;

public interface InputOptimizationView {

    DomainType getDomain();

    String getJsonValue();

}
