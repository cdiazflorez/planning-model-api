package com.mercadolibre.planning.model.api.usecase.unitsdistribution;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.UnitsDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.GetUnitsDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.get.GetUnitsInput;
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
public class GetUnitsDistributionServiceTest {

    private final static String WH = "ARBA01";

    @Mock
    private UnitsDistributionRepository unitsDistributionRepository;

    @InjectMocks
    GetUnitsDistributionService getUnitsDistributionService;

    @Test
    public void getTest(){

        //GIVEN
        ZonedDateTime dateFrom = ZonedDateTime.now();
        ZonedDateTime dateTo = dateFrom.plusHours(4);

        GetUnitsInput getUnitsInput = GetUnitsInput.builder().wareHouseId(WH).dateTo(dateTo).dateFrom(dateFrom).build();

        when(unitsDistributionRepository.findByDateBetweenAndLogisticCenterId(dateFrom,dateTo,WH)).thenReturn(new ArrayList<>());

        //WHEN
        List<UnitsDistribution> response = getUnitsDistributionService.get(getUnitsInput);

        //THEN
        Assertions.assertNotNull(response);
    }


}
