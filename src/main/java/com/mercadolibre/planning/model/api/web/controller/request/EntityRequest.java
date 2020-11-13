package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
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

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EntityRequest {

    @NotBlank
    private String warehouseId;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateFrom;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateTo;

    private Source source;

    @NotEmpty
    private List<ProcessName> processName;

    private Set<ProcessingType> processingType;

    private List<Simulation> simulations;

    public GetEntityInput toGetEntityInput(final Workflow workflow, final EntityType entityType) {
        return GetEntityInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .entityType(entityType)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .source(source)
                .processName(processName)
                .processingType(processingType)
                .simulations(simulations)
                //TODO: Hacer una clase especifica para setear el input de la productividad
                .abilityLevel(Set.of(1))
                .build();
    }
}
