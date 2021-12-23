package com.mercadolibre.planning.model.api.domain.entity.sla;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetSlaByWarehouseOutput {

    private String serviceId;

    private String canalizationId;

    private String logisticCenterId;

    private ZonedDateTime date;

    private ProcessingTime processingTime;
}
