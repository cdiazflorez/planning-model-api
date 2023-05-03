package com.mercadolibre.planning.model.api.projection.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class Quantity {
    @NotNull
    @JsonProperty("date_in")
    Instant dateIn;
    @NotNull
    @JsonProperty("date_out")
    Instant dateOut;
    int total;
}
