package com.mercadolibre.planning.model.api.projection.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.Value;

@Value
public class BacklogProjectionResponse {
    @JsonProperty("operation_hour")
    Instant operationHour;
    List<Backlog> backlog;
}
