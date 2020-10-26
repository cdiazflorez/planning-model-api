package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionInput;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Data
@NoArgsConstructor
public class ProjectionRequest {

    @NotNull
    private String warehouseId;

    @NotNull
    private ProjectionType type;

    @NotNull
    private List<ProcessName> processName;

    @NotNull
    private ZonedDateTime dateFrom;

    @NotNull
    private ZonedDateTime dateTo;

    private List<ProjectionBacklogRequest> backlog;

    public ProjectionInput toProjectionInput(final Workflow workflow) {
        return ProjectionInput.builder()
                .warehouseId(warehouseId)
                .type(type)
                .processName(processName)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .backlog(backlog == null
                        ? emptyList()
                        : backlog.stream()
                        .map(backlogRequest -> new Backlog(
                                backlogRequest.getDate(),
                                backlogRequest.getQuantity()))
                        .collect(toList()))
                .workflow(workflow)
                .build();
    }
}
