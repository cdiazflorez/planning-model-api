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
class LastForecastRepositoryImpTest {

  private static final String WAREHOUSE_ID = "ARTW01";
  private static final String WEEK = "36-2022";

  @InjectMocks
  private LastForecastRepositoryImp lastForecastRepositoryImp;

  @Mock
  private ForecastRepository forecastRepository;

  @Test
  public void getForecastIdByWorkflowOK() {

    final ForecastId a = new ForecastId(2L);

    when(forecastRepository
        .findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND.name(), Set.of(WEEK)))
        .thenReturn(List.of(a));

    final Long id = lastForecastRepositoryImp.getForecastByWorkflow(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND, WEEK);

    Assertions.assertNotNull(id);

  }

  @Test
  public void getForecastIdByWorkflowThrows() {

    when(forecastRepository
        .findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND.name(), Set.of(WEEK)))
        .thenReturn(Collections.emptyList());

    final Executable executable = () -> lastForecastRepositoryImp.getForecastByWorkflow(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND, WEEK);

    assertThrows(ForecastNotFoundException.class, executable);

  }

  @Getter
  @RequiredArgsConstructor
  private static class ForecastId implements ForecastIdView {
    private final Long id;
  }


}