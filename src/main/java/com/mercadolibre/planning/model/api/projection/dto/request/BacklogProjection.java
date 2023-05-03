package com.mercadolibre.planning.model.api.projection.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class BacklogProjection {
    Backlog backlog;
    @JsonProperty("planned_unit")
    PlannedUnit plannedUnit;
    List<Throughput> throughput;
    @NotNull
    @JsonProperty("date_from")
    Instant dateFrom;
    @NotNull
    @JsonProperty("date_to")
    Instant dateTo;
    Workflow workflow;
}
