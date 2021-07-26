package com.mercadolibre.planning.model.api.domain.usecase.cycletime.get;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

@Builder
@Value
public class GetCycleTimeInput {

    private ZonedDateTime cptDate;
    private List<Configuration> configurations;
}
