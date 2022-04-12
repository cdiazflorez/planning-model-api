package com.mercadolibre.planning.model.api.web.controller.unitsdistibution.request;

import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create.UnitsDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create.UnitsInput;
import lombok.*;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UnitsDistributionRequest {

    @NotBlank
    private List<Units> unitsDistribution;

    public UnitsDistributionInput toUnitsDistributionInput(){
        List<UnitsInput> unitsInputs = new ArrayList<>();
        unitsDistribution.forEach(units -> {
            unitsInputs.add(UnitsInput.builder().date(units.getDate())
                    .processName(units.getProcessName())
                    .area(units.getArea())
                    .logisticCenterId(units.getLogisticCenterId())
                    .quantity(units.getQuantity())
                    .quantityMetricUnit(units.getQuantityMetricUnit())
                    .build());
        });

        return  new UnitsDistributionInput(unitsInputs);
    }
}
