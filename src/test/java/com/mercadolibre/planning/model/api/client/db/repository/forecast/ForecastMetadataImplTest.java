package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ForecastMetadataImplTest {
  private static final Instant NOW = Instant.parse("2023-03-17T00:00:00Z");
  private static final String WAREHOUSE_ID = "ARTW01";
  private static final String WEEK = "11-2023";
  @InjectMocks
  private ForecastMetadataImpl forecastMetadata;
  @Mock
  private ForecastRepository forecastRepository;
  private MockedStatic<Instant> now;

  @BeforeEach
  public void setUp() {
    now = mockStatic(Instant.class);
    now.when(Instant::now).thenReturn(NOW);
  }

  @AfterEach
  public void tearDown() {
    now.close();
  }

  @Test
  public void testGetForecastByWorkflowOK() {
    //GIVEN
    when(forecastRepository
        .findForecastIdsByWarehouseIdAAndWorkflowAndWeeksAtViewDate(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND.name(), Set.of(WEEK), NOW))
        .thenReturn(List.of(new ForecastId(2L)));

    //WHEN
    final Long idForecast = forecastMetadata.getForecastByWorkflow(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND, WEEK);

    //THEN
    Assertions.assertNotNull(idForecast);
  }

  @Test
  public void testGetForecastByWorkflowThrows() {
    //GIVEN
    when(forecastRepository
        .findForecastIdsByWarehouseIdAAndWorkflowAndWeeksAtViewDate(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND.name(), Set.of(WEEK), NOW))
        .thenReturn(Collections.emptyList());

    //WHEN
    final Executable executable = () -> forecastMetadata.getForecastByWorkflow(WAREHOUSE_ID, Workflow.FBM_WMS_INBOUND, WEEK);

    //THEN
    assertThrows(ForecastNotFoundException.class, executable);
  }

  @Getter
  @RequiredArgsConstructor
  private static class ForecastId implements ForecastIdView {
    private final Long id;
  }
}
