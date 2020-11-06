package com.mercadolibre.planning.model.api.domain.usecase.input;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Data
@Builder
public class GetEntityInput {

    private String warehouseId;
    private Workflow workflow;
    private EntityType entityType;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private Source source;
    private List<ProcessName> processName;
    private Set<ProcessingType> processingType;
}
