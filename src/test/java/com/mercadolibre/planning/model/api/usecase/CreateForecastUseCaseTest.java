package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.CALLER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCreateForecastInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockMetadatas;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastOutput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationOfWeek;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationService;
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountProductivityGateway;
import com.mercadolibre.planning.model.api.gateway.PlanningDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateForecastUseCaseTest {

  @Mock
  private ForecastGateway forecastGateway;

  @Mock
  private ProcessingDistributionGateway processingDistributionGateway;

  @Mock
  private HeadcountDistributionGateway headcountDistributionGateway;

  @Mock
  private HeadcountProductivityGateway headcountProductivityGateway;

  @Mock
  private PlanningDistributionGateway planningDistributionGateway;

  @Mock
  private DeactivateSimulationService deactivateSimulationService;

  @InjectMocks
  private CreateForecastUseCase createForecastUseCase;

  @Test
  public void emptyTrajectories() {
    // GIVEN
    final Forecast forecast = new Forecast();
    forecast.setWorkflow(FBM_WMS_OUTBOUND);
    forecast.setUserId(1234);

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_OUTBOUND);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInput input = CreateForecastInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .headcountDistributions(List.of())
        .headcountProductivities(List.of())
        .planningDistributions(List.of())
        .processingDistributions(List.of())
        .polyvalentProductivities(List.of())
        .backlogLimits(List.of())
        .metadata(mockMetadatas())
        .userId(CALLER_ID)
        .build();

    // WHEN
    final CreateForecastOutput output = createForecastUseCase.execute(input);

    // THEN
    verifyNoMoreInteractions(processingDistributionGateway,
        headcountDistributionGateway,
        headcountProductivityGateway,
        planningDistributionGateway,
        deactivateSimulationService
    );

    assertEquals(1L, output.getId());
  }

  @Test
  public void nullTrajectories() {
    // GIVEN
    final Forecast forecast = new Forecast();
    forecast.setWorkflow(FBM_WMS_OUTBOUND);
    forecast.setUserId(1234);

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_OUTBOUND);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInput input = CreateForecastInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .headcountDistributions(null)
        .headcountProductivities(null)
        .planningDistributions(null)
        .processingDistributions(null)
        .polyvalentProductivities(null)
        .backlogLimits(null)
        .metadata(mockMetadatas())
        .userId(CALLER_ID)
        .build();

    // WHEN
    final CreateForecastOutput output = createForecastUseCase.execute(input);

    // THEN
    verifyNoMoreInteractions(processingDistributionGateway,
        headcountDistributionGateway,
        headcountProductivityGateway,
        planningDistributionGateway,
        deactivateSimulationService
    );

    assertEquals(1L, output.getId());
  }

  @Test
  @DisplayName("A forecast is created successfully")
  public void createSaveOk() {
    // GIVEN
    final Forecast forecast = new Forecast();
    forecast.setWorkflow(FBM_WMS_OUTBOUND);
    forecast.setUserId(1234);

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_OUTBOUND);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInput input = mockCreateForecastInput();

    // WHEN
    final CreateForecastOutput output = createForecastUseCase.execute(input);

    // THEN
    verify(processingDistributionGateway).create(
        getProcessingDists(savedForecast),
        savedForecast.getId());

    verify(headcountDistributionGateway).create(
        getHeadcountDists(savedForecast),
        savedForecast.getId());
    verify(headcountProductivityGateway).create(
        getAllProductivities(savedForecast),
        savedForecast.getId());
    verify(planningDistributionGateway).create(
        getPlanningDistributions(savedForecast),
        savedForecast.getId());

    verify(deactivateSimulationService).deactivateSimulation(any(DeactivateSimulationOfWeek.class));

    assertEquals(1L, output.getId());
  }

  @Test
  @DisplayName("An empty forecast is created successfully ")
  public void emptySaveOk() {
    // GIVEN
    final Forecast forecast = new Forecast();
    forecast.setWorkflow(FBM_WMS_OUTBOUND);
    forecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_OUTBOUND);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInput input = CreateForecastInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .metadata(mockMetadatas())
        .userId(CALLER_ID)
        .headcountDistributions(List.of())
        .processingDistributions(List.of())
        .planningDistributions(List.of())
        .headcountProductivities(List.of())
        .backlogLimits(List.of())
        .build();

    // WHEN
    final CreateForecastOutput output = createForecastUseCase.execute(input);

    // THEN
    verifyNoInteractions(processingDistributionGateway);
    verifyNoInteractions(headcountDistributionGateway);
    verifyNoInteractions(headcountProductivityGateway);
    verifyNoInteractions(planningDistributionGateway);
    verifyNoInteractions(deactivateSimulationService);

    assertEquals(1L, output.getId());
  }

  private List<ForecastMetadata> getForecastMetadatas() {
    return List.of(
        new ForecastMetadata(0, "warehouse_id", "ARBA01"),
        new ForecastMetadata(0, "week", "26-2020"),
        new ForecastMetadata(0, "mono_order_distribution", "58"),
        new ForecastMetadata(0, "multi_order_distribution", "42")
    );
  }

  private List<ProcessingDistribution> getProcessingDists(final Forecast forecast) {
    return List.of(
        new ProcessingDistribution(0, DATE_IN, ProcessPath.TOT_MONO, WAVING, 172, UNITS, PERFORMED_PROCESSING, forecast),
        new ProcessingDistribution(0, DATE_IN.plusHours(1), ProcessPath.TOT_MONO, WAVING, 295, UNITS, PERFORMED_PROCESSING, forecast),
        new ProcessingDistribution(0, DATE_IN, ProcessPath.GLOBAL, GLOBAL, 1000, UNITS_PER_HOUR, MAX_CAPACITY, forecast),
        new ProcessingDistribution(0, DATE_IN.plusHours(1), ProcessPath.GLOBAL, GLOBAL, 1000, UNITS_PER_HOUR, MAX_CAPACITY, forecast)
    );
  }

  private List<HeadcountDistribution> getHeadcountDists(final Forecast forecast) {
    return List.of(
        new HeadcountDistribution(0, "MZ", PICKING, 85, PERCENTAGE, forecast),
        new HeadcountDistribution(0, "RS", PICKING, 5, PERCENTAGE, forecast),
        new HeadcountDistribution(0, "HV", PICKING, 5, PERCENTAGE, forecast),
        new HeadcountDistribution(0, "BL", PICKING, 5, PERCENTAGE, forecast)
    );
  }

  private List<PlanningDistribution> getPlanningDistributions(final Forecast forecast) {
    return List.of(new PlanningDistribution(
        1, DATE_IN, DATE_OUT, 1200, UNITS, ProcessPath.GLOBAL, forecast, List.of())
    );
  }

  private List<HeadcountProductivity> getAllProductivities(final Forecast forecast) {
    return List.of(
        new HeadcountProductivity(0, A_DATE_UTC, ProcessPath.TOT_MONO, PICKING, 85, UNITS_PER_HOUR, 0, forecast),
        new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), ProcessPath.TOT_MONO, PICKING, 85, UNITS_PER_HOUR, 0, forecast),
        new HeadcountProductivity(0, A_DATE_UTC, ProcessPath.TOT_MONO, PICKING, 73, UNITS_PER_HOUR, 1, forecast),
        new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), ProcessPath.TOT_MONO, PICKING, 73, UNITS_PER_HOUR, 1, forecast),
        new HeadcountProductivity(0, A_DATE_UTC, ProcessPath.GLOBAL, PACKING, 92, UNITS_PER_HOUR, 0, forecast),
        new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), ProcessPath.GLOBAL, PACKING, 85, UNITS_PER_HOUR, 0, forecast),
        new HeadcountProductivity(0, A_DATE_UTC, ProcessPath.GLOBAL, PACKING, 82, UNITS_PER_HOUR, 1, forecast),
        new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), ProcessPath.GLOBAL, PACKING, 76, UNITS_PER_HOUR, 1, forecast)

    );
  }
}
