package com.mercadolibre.planning.model.api.projection.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class Throughput {
    @JsonProperty("operation_hour")
    Instant operationHour;
    List<QuantityByProcessName> quantityByProcessName;

    @Value
    public static class QuantityByProcessName {
        ProcessName name;

        int total;
    }
}
