package com.mercadolibre.planning.model.api.client.db.repository.inputcatalog;

import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;

public interface InputCatalogView {

    InputId getDomain();

    String getJsonValue();

}
