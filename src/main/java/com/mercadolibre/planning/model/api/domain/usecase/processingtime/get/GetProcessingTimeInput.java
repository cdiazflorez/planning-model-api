package com.mercadolibre.planning.model.api.domain.usecase.processingtime.get;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;

@Value
@Builder
public class GetProcessingTimeInput {

    @NotNull
    private String logisticCenterId;

    @NotNull
    private Workflow workflow;

    @NotNull
    private List<ZonedDateTime> cpt;

}
