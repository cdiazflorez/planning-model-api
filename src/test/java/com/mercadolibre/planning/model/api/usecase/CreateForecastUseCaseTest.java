package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.HU_ASSEMBLY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.SALES_DISPATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.CALLER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.WEEK;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCreateForecastInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCreateForecastInputWithTotalWorkersNsType;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockMetadatas;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastOutput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastUseCase;
import com.mercadolibre.planning.model.api.exception.TagsParsingException;
import com.mercadolibre.planning.model.api.web.controller.forecast.dto.CreateForecastInputDto;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationOfWeek;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.deactivate.DeactivateSimulationService;
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountProductivityGateway;
import com.mercadolibre.planning.model.api.gateway.PlanningDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import com.mercadolibre.planning.model.api.web.controller.forecast.dto.StaffingPlanDto;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateForecastUseCaseTest {

  private static final String DATE_KEY = "date";
  private static final String PROCESS_KEY = "process";
  private static final String PROCESS_PATH_KEY = "process_path";
  private static final String HEADCOUNT_TYPE_KEY = "headcount_type";
  private static final String NON_SYSTEMIC = "non_systemic";

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
    forecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    forecast.setWeek(WEEK);
    forecast.setUserId(1234);

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_OUTBOUND);
    savedForecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    savedForecast.setWeek(WEEK);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInputDto input = new CreateForecastInputDto(
        WEEK,
        CALLER_ID,
        FBM_WMS_OUTBOUND,
        LOGISTIC_CENTER_ID,
        mockMetadatas(),
        List.of(),
        List.of(),
        List.of()
    );


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
    forecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    forecast.setWeek(WEEK);
    forecast.setUserId(1234);

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_OUTBOUND);
    savedForecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    savedForecast.setWeek(WEEK);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInputDto input = new CreateForecastInputDto(
        WEEK,
        CALLER_ID,
        FBM_WMS_OUTBOUND,
        LOGISTIC_CENTER_ID,
        mockMetadatas(),
        null,
        null,
        null
    );

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
    forecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    forecast.setWeek(WEEK);
    forecast.setUserId(1234);

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_OUTBOUND);
    savedForecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    savedForecast.setWeek(WEEK);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInputDto input = mockCreateForecastInput(FBM_WMS_OUTBOUND);

    // WHEN
    final CreateForecastOutput output = createForecastUseCase.execute(input);

    // THEN
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
  @DisplayName("A inbound forecast is created successfully")
  public void createInboundForecastSaveOk() {
    // GIVEN
    final Forecast forecast = new Forecast();
    forecast.setWorkflow(FBM_WMS_INBOUND);
    forecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    forecast.setWeek(WEEK);
    forecast.setUserId(1234);

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_INBOUND);
    savedForecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    savedForecast.setWeek(WEEK);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInputDto input = mockCreateForecastInput(FBM_WMS_INBOUND);

    // WHEN
    final CreateForecastOutput output = createForecastUseCase.execute(input);

    // THEN
    verify(headcountDistributionGateway).create(
        getHeadcountDists(savedForecast),
        savedForecast.getId());
    verify(headcountProductivityGateway).create(
        getInboundProductivities(savedForecast),
        savedForecast.getId());

    verify(deactivateSimulationService).deactivateSimulation(any(DeactivateSimulationOfWeek.class));

    assertEquals(1L, output.getId());
  }

  @Test
  @DisplayName("A forecast is created successfully")
  public void createSaveWithTotalWorkersNsTypeOk() throws JsonProcessingException {
    // GIVEN
    final Forecast forecast = new Forecast();
    forecast.setWorkflow(FBM_WMS_OUTBOUND);
    forecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    forecast.setWeek(WEEK);
    forecast.setUserId(1234);

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_OUTBOUND);
    savedForecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    savedForecast.setWeek(WEEK);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInputDto input = mockCreateForecastInputWithTotalWorkersNsType();

    // WHEN
    final CreateForecastOutput output = createForecastUseCase.execute(input);

    // THEN
    verify(processingDistributionGateway).create(
        getProcessingDistsWithTotalWorkersNSType(savedForecast),
        savedForecast.getId());

    assertEquals(1L, output.getId());
  }

  @Test
  @DisplayName("An empty forecast is created successfully ")
  public void emptySaveOk() {
    // GIVEN
    final Forecast forecast = new Forecast();
    forecast.setWorkflow(FBM_WMS_OUTBOUND);
    forecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    forecast.setWeek(WEEK);
    forecast.setUserId(1234);

    final List<ForecastMetadata> forecastMetadatas = getForecastMetadatas();

    final Forecast savedForecast = new Forecast();
    savedForecast.setWorkflow(FBM_WMS_OUTBOUND);
    savedForecast.setLogisticCenterId(LOGISTIC_CENTER_ID);
    savedForecast.setWeek(WEEK);
    savedForecast.setId(1L);
    savedForecast.setUserId(1234);

    when(forecastGateway.create(forecast, forecastMetadatas)).thenReturn(savedForecast);

    final CreateForecastInputDto input = new CreateForecastInputDto(
        WEEK,
        CALLER_ID,
        FBM_WMS_OUTBOUND,
        LOGISTIC_CENTER_ID,
        mockMetadatas(),
        List.of(),
        List.of(),
        List.of()
    );

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

  @Test
  void testTagParsingException() throws JsonProcessingException {
    ObjectMapper om = Mockito.spy(new ObjectMapper());
    when(om.writeValueAsString(any())).thenThrow(new JsonProcessingException("") {});

    final var staffingPlan = new StaffingPlanDto(
        THROUGHPUT,
        Map.of(DATE_KEY, DATE_IN.toString()),
        UNITS_PER_HOUR,
        124
    );
    assertThrows(TagsParsingException.class, () -> staffingPlan.toProcessingDists(mockSimpleForecast(), om));
  }

  private List<ForecastMetadata> getForecastMetadatas() {
    return List.of(
        new ForecastMetadata(0, "mono_order_distribution", "58"),
        new ForecastMetadata(0, "multi_order_distribution", "42")
    );
  }

  private List<ProcessingDistribution> getProcessingDistsWithTotalWorkersNSType(final Forecast forecast) throws JsonProcessingException {
    return List.of(
        createProcessingDistribution(
                DATE_IN, ProcessPath.GLOBAL, WAVING, 172, UNITS, PERFORMED_PROCESSING, forecast,
            Map.of(PROCESS_KEY, WAVING.toJson())
        ),
        createProcessingDistribution(
                DATE_IN.plusHours(1), ProcessPath.GLOBAL, WAVING, 295, UNITS, PERFORMED_PROCESSING, forecast,
            Map.of(PROCESS_KEY, WAVING.toJson())
        ),
        createProcessingDistribution(
                DATE_IN, ProcessPath.GLOBAL, HU_ASSEMBLY, 10, WORKERS, EFFECTIVE_WORKERS_NS, forecast,
           new HashMap<>(Map.of(
                PROCESS_KEY, HU_ASSEMBLY.toJson(),
                PROCESS_PATH_KEY, GLOBAL.toJson(),
                HEADCOUNT_TYPE_KEY, NON_SYSTEMIC
            ))
        ),
        createProcessingDistribution(
                DATE_IN.plusHours(1), ProcessPath.GLOBAL, HU_ASSEMBLY, 10, WORKERS, EFFECTIVE_WORKERS_NS, forecast,
            new HashMap<>(Map.of(
                PROCESS_KEY, HU_ASSEMBLY.toJson(),
                PROCESS_PATH_KEY, GLOBAL.toJson(),
                HEADCOUNT_TYPE_KEY, NON_SYSTEMIC
            ))
        ),
        createProcessingDistribution(
                DATE_IN, ProcessPath.GLOBAL, SALES_DISPATCH, 10, WORKERS, EFFECTIVE_WORKERS_NS, forecast,
            new HashMap<>(Map.of(
                PROCESS_KEY, SALES_DISPATCH.toJson(),
                PROCESS_PATH_KEY, GLOBAL.toJson(),
                HEADCOUNT_TYPE_KEY, NON_SYSTEMIC
            ))
        ),
        createProcessingDistribution(
                DATE_IN.plusHours(1), ProcessPath.GLOBAL, SALES_DISPATCH, 10, WORKERS, EFFECTIVE_WORKERS_NS, forecast,
            new HashMap<>(Map.of(
                PROCESS_KEY, SALES_DISPATCH.toJson(),
                PROCESS_PATH_KEY, GLOBAL.toJson(),
                HEADCOUNT_TYPE_KEY, NON_SYSTEMIC
            ))
        ),
        createProcessingDistribution(
                DATE_IN, ProcessPath.GLOBAL, GLOBAL, 1000, UNITS_PER_HOUR, MAX_CAPACITY, forecast,
            Map.of()
        ),
        createProcessingDistribution(
                DATE_IN.plusHours(1), ProcessPath.GLOBAL, GLOBAL, 1000, UNITS_PER_HOUR, MAX_CAPACITY, forecast,
            Map.of()
        )
    );
  }

  private ProcessingDistribution createProcessingDistribution(
      final ZonedDateTime date,
      final ProcessPath processPath,
      final ProcessName processName,
      final int quantity,
      final MetricUnit metricUnit,
      final ProcessingType processingType,
      final Forecast forecast,
      final Map<String, String> tags
  ) throws JsonProcessingException {
    final ObjectMapper om = new ObjectMapper();
    String tagsString = om.writeValueAsString(tags);
    return ProcessingDistribution.builder()
        .date(date)
        .processPath(processPath)
        .processName(processName)
        .quantity(quantity)
        .quantityMetricUnit(metricUnit)
        .type(processingType)
        .forecast(forecast)
        .tags(tagsString)
        .build();
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

  private List<HeadcountProductivity> getInboundProductivities(final Forecast forecast) {
    return List.of(
        new HeadcountProductivity(0, A_DATE_UTC, ProcessPath.GLOBAL, PICKING, 85, UNITS_PER_HOUR, 0, forecast),
        new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), ProcessPath.GLOBAL, PICKING, 85, UNITS_PER_HOUR, 0, forecast),
        new HeadcountProductivity(0, A_DATE_UTC, ProcessPath.GLOBAL, PICKING, 73, UNITS_PER_HOUR, 1, forecast),
        new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), ProcessPath.GLOBAL, PICKING, 73, UNITS_PER_HOUR, 1, forecast),
        new HeadcountProductivity(0, A_DATE_UTC, ProcessPath.GLOBAL, PACKING, 92, UNITS_PER_HOUR, 0, forecast),
        new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), ProcessPath.GLOBAL, PACKING, 85, UNITS_PER_HOUR, 0, forecast),
        new HeadcountProductivity(0, A_DATE_UTC, ProcessPath.GLOBAL, PACKING, 82, UNITS_PER_HOUR, 1, forecast),
        new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), ProcessPath.GLOBAL, PACKING, 76, UNITS_PER_HOUR, 1, forecast)

    );
  }
}
