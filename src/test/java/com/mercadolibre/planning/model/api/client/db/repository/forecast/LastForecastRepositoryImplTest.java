package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LastForecastRepositoryImplTest {

  private static final String WAREHOUSE_ID = "ARTW01";
  private static final String WEEK = "36-2022";

  @InjectMocks
  private LastForecastRepositoryImpl lastForecastRepositoryImpl;

  @Mock
  private ForecastRepository forecastRepository;

  @Test
  public void testGetForecastByWorkflowOK() {

    //GIVEN
    when(forecastRepository
        .findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND.name(), Set.of(WEEK)))
        .thenReturn(List.of(new ForecastId(2L)));

    //WHEN
    final Long idForecast = lastForecastRepositoryImpl.getForecastByWorkflow(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND, WEEK);

    //THEN
    Assertions.assertNotNull(idForecast);

  }

  @Test
  public void testGetForecastByWorkflowThrows() {

    //GIVEN
    when(forecastRepository
        .findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND.name(), Set.of(WEEK)))
        .thenReturn(Collections.emptyList());

    //WHEN
    final Executable executable = () -> lastForecastRepositoryImpl.getForecastByWorkflow(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND, WEEK);

    //THEN
    assertThrows(ForecastNotFoundException.class, executable);

  }

  @Getter
  @RequiredArgsConstructor
  private static class ForecastId implements ForecastIdView {
    private final Long id;
  }
}
