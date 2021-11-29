package com.mercadolibre.planning.model.api.domain.entity.forecast;

import java.util.Date;

public interface MaxCapacityView {

    String getLogisticCenterId();

    Date getLoadDate();

    Date getMaxCapacityDate();

    long getMaxCapacityValue();
}
