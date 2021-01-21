package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetEntityInput;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityRequest {

    @NotBlank
    protected String warehouseId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    protected ZonedDateTime dateFrom;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    protected ZonedDateTime dateTo;

    protected Source source;

    @NotEmpty
    protected List<ProcessName> processName;

    protected Set<ProcessingType> processingType;

    protected List<Simulation> simulations;

    public GetEntityInput toGetEntityInput(final Workflow workflow) {
        return GetEntityInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .source(source)
                .processName(processName)
                .simulations(simulations)
                .build();
    }
}
