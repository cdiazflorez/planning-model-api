package com.mercadolibre.planning.model.api.usecase.simulation.deactivate;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationOfWeek;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationService;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeactivateSimulationServiceTest {

  @InjectMocks
  private DeactivateSimulationService deactivateSimulationService;

  @Mock
  private CurrentProcessingDistributionRepository currentProcessingDistributionRepository;

  @Mock
  private CurrentHeadcountProductivityRepository currentHeadcountProductivityRepository;

  @Test
  public void deactivateSimulationOkTest() {
    //GIVEN
    final ZonedDateTime dateTo = A_DATE_UTC.plus(10, HOURS);

    final DeactivateSimulationOfWeek deactivateSimulationOfWeek = new DeactivateSimulationOfWeek(
        WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        A_DATE_UTC,
        dateTo,
        USER_ID
    );

    //WHEN
    deactivateSimulationService.deactivateSimulation(deactivateSimulationOfWeek);

    //THEN
    verify(currentProcessingDistributionRepository, times(1)).deactivateProcessingDistributionForRangeOfDates(
        WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        A_DATE_UTC,
        dateTo,
        USER_ID
    );

    verify(currentHeadcountProductivityRepository, times(1)).deactivateProductivityForRangeOfDates(
        WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        A_DATE_UTC,
        dateTo,
        USER_ID
    );
  }

}
