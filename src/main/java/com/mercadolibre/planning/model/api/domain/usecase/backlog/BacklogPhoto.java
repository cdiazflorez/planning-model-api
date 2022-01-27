package com.mercadolibre.planning.model.api.domain.usecase.backlog;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.mercadolibre.planning.model.api.util.CustomInstantDeserializer;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class BacklogPhoto {
    @JsonDeserialize(using = CustomInstantDeserializer.class)
    private Instant date;

    private Map<String, String> keys;

    private Integer total;
}
