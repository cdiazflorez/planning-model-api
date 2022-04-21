package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.UnitsDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.get.GetUnitsInput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class GetUnitsDistributionService {

    private final UnitsDistributionRepository unitsDistributionRepository;

    public List<UnitsDistribution> get(GetUnitsInput getUnitsInput){
        return unitsDistributionRepository.findByDateBetweenAndLogisticCenterId(getUnitsInput.getDateFrom(), getUnitsInput.getDateTo(), getUnitsInput.getWareHouseId());
    }
}
