package com.mercadolibre.planning.model.api.projection.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Throughput {
    @JsonProperty("operation_hour")
    Instant operationHour;
    List<Process> process;
}
