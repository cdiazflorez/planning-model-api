package com.mercadolibre.planning.model.api.domain.entity.sla;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class GetSlaByWarehouseOutput {

    String serviceId;

    String canalizationId;

    String logisticCenterId;

    ZonedDateTime date;

    ProcessingTime processingTime;
}
