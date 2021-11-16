package com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse;

import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class GetCptByWarehouseInput {

    private String logisticCenterId;

    private ZonedDateTime cptFrom;

    private ZonedDateTime cptTo;

    private List<ZonedDateTime> cptDefault;

    private String timeZone;
}
