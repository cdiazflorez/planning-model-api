package com.mercadolibre.planning.model.api.usecase.inputoptimization;

import com.mercadolibre.planning.model.api.client.db.repository.inputoptimization.InputOptimizationView;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InputOptimizationViewImpl implements InputOptimizationView {

    private final DomainType domain;

    private final String jsonValue;

    @Override
    public DomainType getDomain() {
        return this.domain;
    }

    @Override
    public String getJsonValue() {
        return this.jsonValue;
    }
}
