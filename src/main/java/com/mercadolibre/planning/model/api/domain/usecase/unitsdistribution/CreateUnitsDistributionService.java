package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.UnitsDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.forecast.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create.UnitsDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create.UnitsInput;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class CreateUnitsDistributionService {

    private final UnitsDistributionRepository unitsDistributionRepository;

    public List<UnitsDistribution> save(UnitsDistributionInput unitsDistributionInput){

        List<UnitsDistribution> unitsDistributionList = new ArrayList<>();
        for(UnitsInput u : unitsDistributionInput.getUnitsDistribution()){
            UnitsDistribution unitsDistribution = unitsDistributionRepository.getByDate(u.getDate());
            if(unitsDistribution == null || unitsDistribution.getDate() == null){
                unitsDistributionList.add(UnitsDistribution.builder().date(u.getDate())
                        .processName(u.getProcessName())
                        .logisticCenterId(u.getLogisticCenterId())
                        .area(u.getArea())
                        .quantity(u.getQuantity())
                        .quantityMetricUnit(MetricUnit.of(u.getQuantityMetricUnit()).get())
                        .build());

            }
        }

        if(!unitsDistributionList.isEmpty()){
            return unitsDistributionRepository.saveAll(unitsDistributionList);
        }

        return new ArrayList<>();
    }


}
