package com.mercadolibre.planning.model.api.web.controller.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetHeadcountInput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HeadcountRequest extends EntityRequest {

    private Set<ProcessingType> processingType;

    public GetHeadcountInput toGetHeadcountInput(final Workflow workflow) {
        return GetHeadcountInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .source(source)
                .processName(processName)
                .processingType(processingType)
                .simulations(simulations)
                .build();
    }
}
