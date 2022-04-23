package com.mercadolibre.planning.model.api.usecase.unitsdistribution;

import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.metrics.UnitsDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.metrics.UnitsDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.GetUnitsInput;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.UnitsDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.unitsdistribution.UnitsInput;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UnitsDistributionServiceTest {

  private final static String AREA = "MZ-0";

  private final static String METRIC = "PERCENTAGE";

  private final static String WH = "COCU01";

  private final static String WHA = "ARBA01";

  @Mock
  private UnitsDistributionRepository unitsDistributionRepository;

  @InjectMocks
  UnitsDistributionService unitsDistributionService;

  @Test
  public void saveTest() {

    //GIVEN
    ZonedDateTime dateFrom = ZonedDateTime.now();
    ZonedDateTime dateTo = dateFrom.plusHours(3);

    List<UnitsInput> unitsInputList = List.of(new UnitsInput(WH, dateFrom, ProcessName.PICKING, AREA, 0.3, METRIC,Workflow.FBM_WMS_OUTBOUND),
        new UnitsInput(WH, dateTo, ProcessName.PICKING, AREA, 0.7, METRIC, Workflow.FBM_WMS_OUTBOUND));

    List<UnitsInput> unitsInputList2 = List.of(new UnitsInput(WHA, dateFrom, ProcessName.PICKING, AREA, 0.3, METRIC,Workflow.FBM_WMS_OUTBOUND),
        new UnitsInput(WHA, dateTo, ProcessName.PICKING, AREA, 0.7, METRIC,Workflow.FBM_WMS_OUTBOUND));

    List<UnitsDistribution> unitsDistributionList =
        List.of(new UnitsDistribution(null, WHA, dateFrom, ProcessName.PICKING, AREA, 0.3, MetricUnit.PERCENTAGE,Workflow.FBM_WMS_OUTBOUND),
            new UnitsDistribution(null, WHA, dateTo, ProcessName.PICKING, AREA, 0.7, MetricUnit.PERCENTAGE,Workflow.FBM_WMS_OUTBOUND));

    when(unitsDistributionRepository.findByDateBetweenAndLogisticCenterId(dateFrom, dateTo, WH)).thenReturn(new ArrayList<>());
    when(unitsDistributionRepository.findByDateBetweenAndLogisticCenterId(dateFrom, dateTo, WHA)).thenReturn(unitsDistributionList);

    //WHEN
    List<UnitsDistribution> response = unitsDistributionService.save(unitsInputList);
    List<UnitsDistribution> response2 = unitsDistributionService.save(unitsInputList2);

    //THEN
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(response2);
  }

  @Test
  public void getTest() {

    //GIVEN
    ZonedDateTime dateFrom = ZonedDateTime.now();
    ZonedDateTime dateTo = dateFrom.plusHours(4);

    GetUnitsInput getUnitsInput = new GetUnitsInput(dateFrom, dateTo, WH);

    when(unitsDistributionRepository.findByDateBetweenAndLogisticCenterId(dateFrom, dateTo, WH)).thenReturn(new ArrayList<>());

    //WHEN
    List<UnitsDistribution> response = unitsDistributionService.get(getUnitsInput);

    //THEN
    Assertions.assertNotNull(response);
  }


}
