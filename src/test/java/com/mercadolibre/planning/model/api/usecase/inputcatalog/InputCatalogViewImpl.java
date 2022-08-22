package com.mercadolibre.planning.model.api.usecase.inputcatalog;

import com.mercadolibre.planning.model.api.client.db.repository.inputcatalog.InputCatalogView;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InputCatalogViewImpl implements InputCatalogView {

    private final InputId domain;

    private final String jsonValue;

    @Override
    public InputId getDomain() {
        return this.domain;
    }

    @Override
    public String getJsonValue() {
        return this.jsonValue;
    }
}
