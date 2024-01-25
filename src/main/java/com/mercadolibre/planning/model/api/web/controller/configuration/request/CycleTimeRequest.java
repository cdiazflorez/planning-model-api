package com.mercadolibre.planning.model.api.web.controller.configuration.request;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

public record CycleTimeRequest(@NotNull Set<Workflow> workflows, @NotNull List<Instant> slas) {
}
