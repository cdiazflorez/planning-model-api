package com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.UnitsDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.forecast.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create.UnitsDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create.UnitsInput;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class CreateUnitsDistributionService {

    private final UnitsDistributionRepository unitsDistributionRepository;

    public List<UnitsDistribution> save(UnitsDistributionInput unitsDistributionInput){

        ZonedDateTime start = unitsDistributionInput.getUnitsDistribution()
                .stream()
                .min(Comparator.comparing(UnitsInput::getDate))
                .orElseThrow()
                .getDate();
        ZonedDateTime end = unitsDistributionInput.getUnitsDistribution()
                .stream()
                .max(Comparator.comparing(UnitsInput::getDate))
                .orElseThrow().getDate();


        Set<ZonedDateTime> zonedDateTimes = unitsDistributionRepository.findByDateBetweenaAndLogisticCenterId(start,end,unitsDistributionInput.getUnitsDistribution().get(0).getLogisticCenterId())
                .stream()
                .map(UnitsDistribution::getDate)
                .collect(Collectors.toSet());


        List<UnitsDistribution> unitsDistributionList =  unitsDistributionInput.getUnitsDistribution()
                .stream()
                .filter(unitsInput -> !zonedDateTimes.contains(unitsInput.getDate()))
                .map( u -> UnitsDistribution.builder().date(u.getDate())
                        .processName(u.getProcessName())
                        .logisticCenterId(u.getLogisticCenterId())
                        .area(u.getArea())
                        .quantity(u.getQuantity())
                        .quantityMetricUnit(MetricUnit.of(u.getQuantityMetricUnit()).get())
                        .build()).collect(Collectors.toList());


        if(!unitsDistributionList.isEmpty()){
            return unitsDistributionRepository.saveAll(unitsDistributionList);
        }

        return new ArrayList<>();
    }


}
