package com.mercadolibre.planning.model.api.domain.entity.sla;

import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Value
public class GetSlaByWarehouseInput {

    private String logisticCenterId;

    private ZonedDateTime cptFrom;

    private ZonedDateTime cptTo;

    private List<ZonedDateTime> dafaultSlas;

    private String timeZone;
}
