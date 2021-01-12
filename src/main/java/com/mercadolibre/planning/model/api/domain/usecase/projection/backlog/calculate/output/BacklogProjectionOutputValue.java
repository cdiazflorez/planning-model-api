package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Builder
@Value
public class BacklogProjectionOutputValue {

    private ZonedDateTime date;
    private long quantity;
}
