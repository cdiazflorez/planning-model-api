package com.mercadolibre.planning.model.api.usecase.unitsdistribution;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.UnitsDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.forecast.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.CreateUnitsDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.create.UnitsInput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateUnitsDistributionServiceTest {

    private final static String process = "PICKING";
    private final static String area = "MZ-0";
    private final static String metric = "PERCENTAGE";
    private final static String WH = "COCU01";
    private final static String WHA = "ARBA01";

    @Mock
    private  UnitsDistributionRepository unitsDistributionRepository;

    @InjectMocks
    CreateUnitsDistributionService createUnitsDistributionService;

    @Test
    public void saveTest(){

        //GIVEN
        ZonedDateTime dateFrom = ZonedDateTime.now();
        ZonedDateTime dateTo = dateFrom.plusHours(3);

        List<UnitsInput> unitsInputList = List.of(UnitsInput.builder()
                .date(dateFrom)
                .processName(process)
                .area(area)
                .logisticCenterId(WH)
                .quantity(0.3)
                .quantityMetricUnit(metric)
                .build(),
                UnitsInput.builder()
                        .date(dateTo)
                        .processName(process)
                        .area(area)
                        .logisticCenterId(WH)
                        .quantity(0.7)
                        .quantityMetricUnit(metric)
                        .build());

        List<UnitsInput> unitsInputList2 = List.of(UnitsInput.builder()
                        .date(dateFrom)
                        .processName(process)
                        .area(area)
                        .logisticCenterId(WHA)
                        .quantity(0.3)
                        .quantityMetricUnit(metric)
                        .build(),
                UnitsInput.builder()
                        .date(dateTo)
                        .processName(process)
                        .area(area)
                        .logisticCenterId(WHA)
                        .quantity(0.7)
                        .quantityMetricUnit(metric)
                        .build());

        List<UnitsDistribution> unitsDistributionList = List.of(UnitsDistribution.builder()
                        .date(dateFrom)
                        .processName(process)
                        .area(area)
                        .logisticCenterId(WHA)
                        .quantity(0.3)
                        .quantityMetricUnit(MetricUnit.PERCENTAGE)
                        .build(),
                UnitsDistribution.builder()
                        .date(dateTo)
                        .processName(process)
                        .area(area)
                        .logisticCenterId(WHA)
                        .quantity(0.7)
                        .quantityMetricUnit(MetricUnit.PERCENTAGE)
                        .build());

        when(unitsDistributionRepository.findByDateBetweenAndLogisticCenterId(dateFrom,dateTo,WH)).thenReturn(new ArrayList<>());
        when(unitsDistributionRepository.findByDateBetweenAndLogisticCenterId(dateFrom,dateTo,WHA)).thenReturn(unitsDistributionList);

        //WHEN
        List<UnitsDistribution> response = createUnitsDistributionService.save(unitsInputList);
        List<UnitsDistribution> response2 = createUnitsDistributionService.save(unitsInputList2);

        //THEN
        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response2);
    }


}
