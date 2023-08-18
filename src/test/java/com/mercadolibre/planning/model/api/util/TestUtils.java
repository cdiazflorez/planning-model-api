package com.mercadolibre.planning.model.api.util;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.HU_ASSEMBLY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.RECEIVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.SALES_DISPATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.BACKLOG_LOWER_LIMIT;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.BACKLOG_UPPER_LIMIT;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.ABSENCES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.BACKLOG_BOUNDS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.CONFIGURATION;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.CONTRACT_MODALITY_TYPES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.NON_SYSTEMIC_RATIO;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.POLYVALENCE_PARAMETERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.PRESENCES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.SHIFTS_PARAMETERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.SHIFT_CONTRACT_MODALITIES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.TRANSFERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.WORKERS_COSTS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.WORKERS_PARAMETERS;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.lang.Boolean.TRUE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.MaxCapacityView;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.create.CreateForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanDistribution;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.AreaRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountProductivityDataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.HeadcountProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.MetadataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.PlanningDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.PolyvalentProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionDataRequest;
import com.mercadolibre.planning.model.api.web.controller.forecast.request.ProcessingDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;

@SuppressWarnings({
    "PMD.ExcessiveImports",
    "PMD.CouplingBetweenObjects",
    "PMD.GodClass",
    "PMD.ExcessivePublicCount"
})
public final class TestUtils {

  public static final ZonedDateTime A_DATE_UTC = ZonedDateTime.of(2020, 8, 19, 17, 0, 0, 0,
      ZoneId.of("UTC"));

  public static final ZonedDateTime DATE_IN = ZonedDateTime.of(2020, 8, 19, 18, 0, 0, 0,
      ZoneId.of("UTC"));

  public static final ZonedDateTime DATE_OUT = ZonedDateTime.of(2020, 8, 20, 15, 30, 0, 0,
      ZoneId.of("UTC"));

  public static final ZonedDateTime DEACTIVATE_DATE_FROM = ZonedDateTime.now();

  public static final ZonedDateTime DEACTIVATE_DATE_TO = DEACTIVATE_DATE_FROM.plus(3, DAYS);

  public static final String FORECAST_METADATA_KEY = "mono_order_distribution";

  public static final String FORECAST_METADATA_VALUE = "48";

  public static final String PLANNING_METADATA_KEY = "carrier";

  public static final String PLANNING_METADATA_VALUE = "Mercado envíos";

  public static final String WAREHOUSE_ID = "ARBA01";

  public static final String LOGISTIC_CENTER_ID = "ARBA01";

  public static final String CONFIG_KEY = "expedition_processing_time";

  public static final long USER_ID = 1234L;

  public static final long CALLER_ID = 1234;

  public static final Integer LIMIT = 1;

  private TestUtils() {
  }

  public static Forecast mockForecast(final Set<HeadcountDistribution> headcountDists,
                                      final Set<HeadcountProductivity> productivities,
                                      final Set<PlanningDistribution> planningDists,
                                      final Set<ProcessingDistribution> procDists,
                                      final Set<ForecastMetadata> forecastMetadatas,
                                      final long callerId) {
    return forecastEntityBuilder()
        .headcountDistributions(headcountDists)
        .processingDistributions(procDists)
        .headcountProductivities(productivities)
        .planningDistributions(planningDists)
        .metadatas(forecastMetadatas)
        .userId(callerId)
        .build();
  }

  public static Forecast mockSimpleForecast() {
    return forecastEntityBuilder().build();
  }

  public static Forecast.ForecastBuilder forecastEntityBuilder() {
    return Forecast.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .dateCreated(A_DATE_UTC)
        .lastUpdated(A_DATE_UTC)
        .userId(CALLER_ID);
  }

  public static PlanningDistribution mockPlanningDist(final Forecast forecast) {
    return planningBuilder().forecast(forecast).build();
  }

  public static PlanningDistribution.PlanningDistributionBuilder planningBuilder() {
    return PlanningDistribution.builder()
        .dateIn(DATE_IN)
        .dateOut(DATE_OUT)
        .quantity(1200)
        .metadatas(emptyList())
        .quantityMetricUnit(UNITS);
  }

  public static HeadcountDistribution.HeadcountDistributionBuilder headcountDistBuilder() {
    return HeadcountDistribution.builder()
        .area("MZ")
        .processName(PICKING)
        .quantity(40)
        .quantityMetricUnit(WORKERS);
  }

  public static HeadcountDistribution mockHeadcountDist(final Forecast forecast) {

    return headcountDistBuilder().forecast(forecast).build();
  }

  public static HeadcountProductivity.HeadcountProductivityBuilder headcountProdBuilder() {
    return HeadcountProductivity.builder()
        .abilityLevel(1)
        .productivity(80)
        .productivityMetricUnit(PERCENTAGE)
        .processName(PACKING)
        .date(A_DATE_UTC);
  }

  public static HeadcountProductivity mockHeadcountProd(final Forecast forecast) {
    return headcountProdBuilder().forecast(forecast).build();
  }

  public static ProcessingDistribution.ProcessingDistBuilder processDistBuilder() {
    return ProcessingDistribution.builder()
        .processName(WAVING)
        .quantity(1000)
        .quantityMetricUnit(UNITS)
        .type(REMAINING_PROCESSING)
        .date(A_DATE_UTC);
  }

  public static ProcessingDistribution mockProcessingDist(final Forecast forecast) {
    return processDistBuilder().forecast(forecast).build();
  }

  public static ForecastMetadata.ForecastMetadataBuilder forecastMetadataBuilder() {
    return ForecastMetadata.builder()
        .key(FORECAST_METADATA_KEY)
        .value(FORECAST_METADATA_VALUE);
  }

  public static ForecastMetadata mockForecastMetadata(final Forecast forecast) {
    if (forecast == null) {
      return forecastMetadataBuilder().build();
    }

    return forecastMetadataBuilder().forecastId(forecast.getId()).build();
  }

  public static PlanningDistributionMetadata
      .PlanningDistributionMetadataBuilder planningMetadataBuilder() {

    return PlanningDistributionMetadata.builder()
        .key(PLANNING_METADATA_KEY)
        .value(PLANNING_METADATA_VALUE);
  }

  public static PlanningDistributionMetadata mockPlanningDistMetadata(
      final PlanningDistribution planningDistEntity) {

    if (planningDistEntity == null) {
      return planningMetadataBuilder().build();
    }

    return planningMetadataBuilder()
        .planningDistributionId(planningDistEntity.getId())
        .build();
  }

  public static CurrentHeadcountProductivity mockCurrentProdEntity(final ZonedDateTime date,
                                                                   final long value) {
    return CurrentHeadcountProductivity.builder()
        .abilityLevel(1)
        .date(date)
        .isActive(true)
        .productivity(value)
        .productivityMetricUnit(UNITS_PER_HOUR)
        .processName(PICKING)
        .logisticCenterId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .logisticCenterId(WAREHOUSE_ID)
        .build();
  }

  public static List<CurrentHeadcountProductivity> mockCurrentProductivities() {
    return List.of(
        CurrentHeadcountProductivity.builder()
            .abilityLevel(1)
            .date(DEACTIVATE_DATE_FROM.minus(1, DAYS))
            .isActive(true)
            .productivity(10)
            .productivityMetricUnit(UNITS_PER_HOUR)
            .processName(PICKING)
            .logisticCenterId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .logisticCenterId(WAREHOUSE_ID)
            .build(),
        CurrentHeadcountProductivity.builder()
            .abilityLevel(1)
            .date(DEACTIVATE_DATE_FROM.plus(1, DAYS))
            .isActive(true)
            .productivity(10)
            .productivityMetricUnit(UNITS_PER_HOUR)
            .processName(PICKING)
            .logisticCenterId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .logisticCenterId(WAREHOUSE_ID)
            .build(),
        CurrentHeadcountProductivity.builder()
            .abilityLevel(1)
            .date(DEACTIVATE_DATE_FROM.plus(1, DAYS))
            .isActive(true)
            .productivity(10)
            .productivityMetricUnit(UNITS_PER_HOUR)
            .processName(PUT_AWAY)
            .logisticCenterId(WAREHOUSE_ID)
            .workflow(FBM_WMS_INBOUND)
            .logisticCenterId(WAREHOUSE_ID)
            .build(),
        CurrentHeadcountProductivity.builder()
            .abilityLevel(1)
            .date(DEACTIVATE_DATE_FROM.plus(4, DAYS))
            .isActive(true)
            .productivity(10)
            .productivityMetricUnit(UNITS_PER_HOUR)
            .processName(PICKING)
            .logisticCenterId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .logisticCenterId(WAREHOUSE_ID)
            .build()
    );
  }

  public static CurrentProcessingDistribution mockCurrentProcDist(final ZonedDateTime date, final long value) {
    return CurrentProcessingDistribution.builder()
        .date(date)
        .isActive(true)
        .processPath(GLOBAL)
        .processName(PACKING)
        .quantity(value)
        .quantityMetricUnit(WORKERS)
        .type(EFFECTIVE_WORKERS)
        .workflow(FBM_WMS_OUTBOUND)
        .logisticCenterId(WAREHOUSE_ID)
        .build();
  }

  public static List<CurrentProcessingDistribution> mockCurrentProcessingDistributions() {
    return List.of(
        CurrentProcessingDistribution.builder()
            .date(DEACTIVATE_DATE_FROM.minus(1, DAYS))
            .isActive(true)
            .processName(PACKING)
            .quantity(10)
            .quantityMetricUnit(WORKERS)
            .type(EFFECTIVE_WORKERS)
            .workflow(FBM_WMS_OUTBOUND)
            .logisticCenterId(WAREHOUSE_ID)
            .build(),
        CurrentProcessingDistribution.builder()
            .date(DEACTIVATE_DATE_FROM.plus(1, DAYS))
            .isActive(true)
            .processName(PACKING)
            .quantity(10)
            .quantityMetricUnit(WORKERS)
            .type(EFFECTIVE_WORKERS)
            .workflow(FBM_WMS_OUTBOUND)
            .logisticCenterId(WAREHOUSE_ID)
            .build(),
        CurrentProcessingDistribution.builder()
            .date(DEACTIVATE_DATE_FROM.plus(1, DAYS))
            .isActive(true)
            .processName(PUT_AWAY)
            .quantity(10)
            .quantityMetricUnit(WORKERS)
            .type(EFFECTIVE_WORKERS)
            .workflow(FBM_WMS_INBOUND)
            .logisticCenterId(WAREHOUSE_ID)
            .build(),
        CurrentProcessingDistribution.builder()
            .date(DEACTIVATE_DATE_FROM.plus(4, DAYS))
            .isActive(true)
            .processName(PACKING)
            .quantity(10)
            .quantityMetricUnit(WORKERS)
            .type(EFFECTIVE_WORKERS)
            .workflow(FBM_WMS_OUTBOUND)
            .logisticCenterId(WAREHOUSE_ID)
            .build()
    );
  }

  public static List<PlanDistribution> planningDistributions() {
    return List.of(
        new PlanDistribution(
            2,
            A_DATE_UTC.toInstant(),
            A_DATE_UTC.plusDays(1).toInstant(),
            TOT_MONO,
            UNITS,
            1000),
        new PlanDistribution(
            2,
            A_DATE_UTC.toInstant(),
            A_DATE_UTC.plusDays(1).toInstant(),
            TOT_MULTI_BATCH,
            UNITS,
            300),
        new PlanDistribution(
            1,
            A_DATE_UTC.toInstant(),
            A_DATE_UTC.plusDays(2).toInstant(),
            GLOBAL,
            UNITS,
            1200),
        new PlanDistribution(
            1,
            A_DATE_UTC.plusDays(1).toInstant(),
            A_DATE_UTC.plusDays(2).toInstant(),
            GLOBAL,
            UNITS,
            1250),
        new PlanDistribution(
            1,
            A_DATE_UTC.toInstant(),
            A_DATE_UTC.plusDays(1).toInstant(),
            GLOBAL,
            UNITS,
            500)
    );
  }

  public static List<CurrentPlanningDistribution> currentPlanningDistributions() {
    final CurrentPlanningDistribution first = mock(CurrentPlanningDistribution.class);
    final CurrentPlanningDistribution second = mock(CurrentPlanningDistribution.class);

    when(first.getDateOut()).thenReturn(A_DATE_UTC);

    when(second.getDateOut()).thenReturn(A_DATE_UTC.plusDays(2));
    when(second.getDateInFrom()).thenReturn(A_DATE_UTC.plusDays(1));
    when(second.getQuantity()).thenReturn(2500L);

    return List.of(first, second);
  }

  public static CreateForecastInput mockCreateForecastInput() {
    return CreateForecastInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .headcountDistributions(mockHeadcounts())
        .headcountProductivities(mockProductivities())
        .planningDistributions(mockPlanningDistributions())
        .processingDistributions(mockProcessingDistributions())
        .polyvalentProductivities(mockPolyvalentProductivities())
        .backlogLimits(mockBacklogLimits())
        .metadata(mockMetadatas())
        .userId(CALLER_ID)
        .build();
  }

  public static CreateForecastInput mockCreateForecastInputWithTotalWorkersNsType() {
    return CreateForecastInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .headcountDistributions(mockHeadcounts())
        .headcountProductivities(mockProductivities())
        .planningDistributions(mockPlanningDistributions())
        .processingDistributions(mockProcessingDistributionsWithActiveWorkersNsType())
        .polyvalentProductivities(mockPolyvalentProductivities())
        .backlogLimits(mockBacklogLimits())
        .metadata(mockMetadatas())
        .userId(CALLER_ID)
        .build();
  }

  public static SaveDeviationInput mockSaveForecastDeviationInput() {
    return SaveDeviationInput
        .builder()
        .workflow(FBM_WMS_OUTBOUND)
        .dateFrom(DATE_IN)
        .dateTo(DATE_OUT)
        .value(5.6)
        .userId(USER_ID)
        .warehouseId(WAREHOUSE_ID)
        .build();
  }

  public static SaveDeviationInput mockSaveDeviationInput(final Workflow workflow,
                                                          final DeviationType deviationType,
                                                          final List<Path> paths,
                                                          final double value,
                                                          final ZonedDateTime dateIn,
                                                          final ZonedDateTime dateOut) {

    if (FBM_WMS_OUTBOUND.equals(workflow)) {
      return SaveDeviationInput
          .builder()
          .workflow(workflow)
          .dateFrom(dateIn)
          .dateTo(dateOut)
          .deviationType(deviationType)
          .value(value)
          .userId(USER_ID)
          .warehouseId(WAREHOUSE_ID)
          .build();
    }

    return SaveDeviationInput
        .builder()
        .workflow(workflow)
        .dateFrom(dateIn)
        .dateTo(dateOut)
        .deviationType(deviationType)
        .value(value)
        .paths(paths)
        .userId(USER_ID)
        .warehouseId(WAREHOUSE_ID)
        .build();
  }

  public static DisableForecastDeviationInput mockDisableForecastDeviationInput(final Workflow workflow,
                                                                                final DeviationType deviationType) {
    return new DisableForecastDeviationInput(WAREHOUSE_ID, workflow, deviationType, null);
  }

  public static DisableForecastDeviationInput mockDisableForecastDeviationInputWithAllArgs(final Workflow workflow,
                                                                                           final DeviationType deviationType,
                                                                                           final List<Path> affectedShipmentTypes) {
    return new DisableForecastDeviationInput(WAREHOUSE_ID, workflow, deviationType, affectedShipmentTypes);
  }

  public static GetHeadcountInput mockGetHeadcountEntityInput(final Source source) {
    return mockGetHeadcountEntityInput(source, null, null);
  }

  public static GetHeadcountInput mockGetHeadcountEntityInput(
      final Source source,
      final Set<ProcessingType> processingTypes,
      final List<Simulation> simulations
  ) {
    return mockGetHeadcountEntityInput(List.of(GLOBAL), source, processingTypes, simulations);
  }

  public static GetHeadcountInput mockGetHeadcountEntityInput(
      final List<ProcessPath> processPaths,
      final Source source,
      final Set<ProcessingType> processingTypes,
      final List<Simulation> simulations
  ) {
    return GetHeadcountInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .entityType(HEADCOUNT)
        .dateFrom(A_DATE_UTC)
        .dateTo(A_DATE_UTC.plusDays(2))
        .source(source)
        .processPaths(processPaths)
        .processName(List.of(PICKING, PACKING))
        .processingType(processingTypes)
        .simulations(simulations)
        .viewDate(A_DATE_UTC.toInstant())
        .build();
  }

  public static GetProductivityInput mockGetProductivityEntityInput(
      final Source source,
      final List<Simulation> simulations,
      final List<ProcessPath> processPaths) {
    return GetProductivityInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .entityType(HEADCOUNT)
        .dateFrom(A_DATE_UTC)
        .dateTo(A_DATE_UTC.plusDays(2))
        .source(source)
        .processName(List.of(PICKING, PACKING))
        .processPaths(processPaths)
        .simulations(simulations)
        .abilityLevel(Set.of(1))
        .viewDate(A_DATE_UTC.toInstant())
        .build();
  }

  public static GetEntityInput mockGetThroughputEntityInput(final Source source,
                                                            final List<Simulation> simulations) {
    return GetEntityInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .entityType(HEADCOUNT)
        .dateFrom(A_DATE_UTC)
        .dateTo(A_DATE_UTC.plusDays(1))
        .source(source)
        .processName(List.of(WAVING, PICKING, PACKING, RECEIVING))
        .simulations(simulations)
        .build();
  }

  public static GetPlanningDistributionInput mockPlanningDistributionInput(
      final Instant dateInFrom,
      final Instant dateInTo,
      final Instant dateOutFrom,
      final Instant dateOutTo,
      final Instant viewDate,
      final boolean applyDeviation,
      final Set<ProcessPath> processPaths,
      final boolean applyDeferrals) {

    return GetPlanningDistributionInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .dateInFrom(dateInFrom)
        .dateInTo(dateInTo)
        .dateOutFrom(dateOutFrom)
        .dateOutTo(dateOutTo)
        .viewDate(viewDate)
        .processPaths(processPaths)
        .applyDeviation(applyDeviation)
        .applyDeferrals(applyDeferrals)
        .build();
  }

  public static List<EntityOutput> mockHeadcountEntityOutput() {
    return List.of(
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(WORKERS)
            .source(FORECAST)
            .value(50)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(WORKERS)
            .source(FORECAST)
            .value(40)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(WORKERS)
            .source(FORECAST)
            .value(60)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(WORKERS)
            .source(FORECAST)
            .value(30)
            .build()
    );
  }

  public static List<EntityOutput> mockHeadcountEntityOutputWhitSimulations() {
    return List.of(
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(WORKERS)
            .source(FORECAST)
            .value(50)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(WORKERS)
            .source(FORECAST)
            .value(40)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(WORKERS)
            .source(SIMULATION)
            .value(60)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(WORKERS)
            .source(SIMULATION)
            .value(30)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(WORKERS)
            .source(FORECAST)
            .value(60)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(WORKERS)
            .source(SIMULATION)
            .value(70)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(WORKERS)
            .source(SIMULATION)
            .value(40)
            .build()
    );
  }

  public static List<ProductivityOutput> mockProductivityEntityOutput() {
    return List.of(
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(80)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(70)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(85)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(90)
            .abilityLevel(1)
            .build()
    );
  }

  public static List<ProductivityOutput> mockProductivityEntityOutputWithSimulations() {
    return List.of(
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(80)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(70)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(SIMULATION)
            .value(90)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(SIMULATION)
            .value(50)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(85)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(90)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(SIMULATION)
            .value(75)
            .abilityLevel(1)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(SIMULATION)
            .value(100)
            .abilityLevel(1)
            .build()
    );
  }

  public static List<ProductivityOutput> mockMultiFunctionalProductivityEntityOutput() {
    return List.of(
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(40)
            .abilityLevel(2)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(35)
            .abilityLevel(2)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(40)
            .abilityLevel(2)
            .build(),
        ProductivityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(45)
            .abilityLevel(2)
            .build()
    );
  }

  public static List<EntityOutput> mockThroughputEntityOutput() {
    return List.of(
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(800)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PICKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(600)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC)
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(700)
            .build(),
        EntityOutput.builder()
            .workflow(FBM_WMS_OUTBOUND)
            .date(A_DATE_UTC.plusHours(1))
            .processPath(GLOBAL)
            .processName(PACKING)
            .metricUnit(UNITS_PER_HOUR)
            .source(FORECAST)
            .value(550)
            .build()
    );
  }

  public static List<GetPlanningDistributionOutput> mockGetPlanningDistOutput(final boolean applyDeferrals) {
      if (applyDeferrals) {
          return mockGetPlanningDistributionOutputList();
      }
      return List.of(
              mockGetPlanningDistributionOutputList().get(0),
              mockGetPlanningDistributionOutputList().get(1),
              new GetPlanningDistributionOutput(A_DATE_UTC.toInstant(),
                      A_DATE_UTC.plusDays(3).toInstant(),
                      UNITS,
                      GLOBAL,
                      1700)
      );
  }

  public static List<GetPlanningDistributionOutput> mockGetPlanningDistributionOutputList() {
    return List.of(
        new GetPlanningDistributionOutput(A_DATE_UTC.toInstant(),
            A_DATE_UTC.plusDays(1).toInstant(),
            UNITS,
            GLOBAL,
            1500),
        new GetPlanningDistributionOutput(A_DATE_UTC.toInstant(),
            A_DATE_UTC.plusDays(2).toInstant(),
            UNITS,
            GLOBAL,
            1800)
    );
  }

  public static String getResourceAsString(final String resourceName) throws IOException {
    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    try (InputStream resource = classLoader.getResourceAsStream(resourceName)) {
      assert resource != null;
      return IOUtils.toString(resource, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static List<ProcessingDistributionRequest> mockProcessingDistributions() {
    return List.of(
        new ProcessingDistributionRequest(
            ProcessPath.TOT_MONO,
            WAVING,
            PERFORMED_PROCESSING,
            UNITS,
            List.of(
                new ProcessingDistributionDataRequest(DATE_IN, 172),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 295)
            )),
        new ProcessingDistributionRequest(
            GLOBAL,
            ProcessName.GLOBAL,
            MAX_CAPACITY,
            UNITS_PER_HOUR,
            List.of(
                new ProcessingDistributionDataRequest(DATE_IN, 1000),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 1000)
            ))
    );
  }

  private static List<ProcessingDistributionRequest> mockProcessingDistributionsWithActiveWorkersNsType() {
    return List.of(
        new ProcessingDistributionRequest(
            ProcessPath.TOT_MONO,
            WAVING,
            PERFORMED_PROCESSING,
            UNITS,
            List.of(
                new ProcessingDistributionDataRequest(DATE_IN, 172),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 295)
            )),
        new ProcessingDistributionRequest(
            GLOBAL,
            HU_ASSEMBLY,
            EFFECTIVE_WORKERS_NS,
            WORKERS,
            List.of(
                new ProcessingDistributionDataRequest(DATE_IN, 10),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 10)
            )),
        new ProcessingDistributionRequest(
            GLOBAL,
            SALES_DISPATCH,
            EFFECTIVE_WORKERS_NS,
            WORKERS,
            List.of(
                new ProcessingDistributionDataRequest(DATE_IN, 10),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 10)
            )),
        new ProcessingDistributionRequest(
            GLOBAL,
            ProcessName.GLOBAL,
            MAX_CAPACITY,
            UNITS_PER_HOUR,
            List.of(
                new ProcessingDistributionDataRequest(DATE_IN, 1000),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 1000)
            ))
    );
  }

  private static List<ProcessingDistributionRequest> mockBacklogLimits() {
    return List.of(
        new ProcessingDistributionRequest(
            GLOBAL,
            WAVING,
            BACKLOG_LOWER_LIMIT,
            MINUTES,
            List.of(
                new ProcessingDistributionDataRequest(DATE_IN, 172),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 295)
            )),
        new ProcessingDistributionRequest(
            GLOBAL,
            PICKING,
            BACKLOG_UPPER_LIMIT,
            MINUTES,
            List.of(
                new ProcessingDistributionDataRequest(DATE_IN, 172),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 295)
            )),
        new ProcessingDistributionRequest(
            GLOBAL,
            PACKING,
            BACKLOG_LOWER_LIMIT,
            MINUTES,
            List.of(
                new ProcessingDistributionDataRequest(DATE_IN, 172),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 295)
            ))
    );
  }

  private static List<PolyvalentProductivityRequest> mockPolyvalentProductivities() {
    return asList(
        new PolyvalentProductivityRequest(PACKING, PERCENTAGE, 90, 1),
        new PolyvalentProductivityRequest(PICKING, PERCENTAGE, 86, 1)
    );
  }


  private static List<PlanningDistributionRequest> mockPlanningDistributions() {
    return singletonList(
        new PlanningDistributionRequest(
            DATE_IN, DATE_OUT, UNITS, GLOBAL, 1200, asList(
            new MetadataRequest("carrier_id", "17502740"),
            new MetadataRequest("service_id", "851"),
            new MetadataRequest("canalization", "U")
        ))
    );
  }

  public static List<MetadataRequest> mockMetadatas() {
    return asList(
        new MetadataRequest("warehouse_id", WAREHOUSE_ID),
        new MetadataRequest("week", "26-2020"),
        new MetadataRequest("mono_order_distribution", "58"),
        new MetadataRequest("multi_order_distribution", "42")
    );
  }

  private static List<HeadcountProductivityRequest> mockProductivities() {
    return asList(
        new HeadcountProductivityRequest(ProcessPath.TOT_MONO, PICKING, UNITS_PER_HOUR, 0, List.of(
            new HeadcountProductivityDataRequest(A_DATE_UTC, 85),
            new HeadcountProductivityDataRequest(A_DATE_UTC.plusHours(1), 85)
        )),
        new HeadcountProductivityRequest(GLOBAL, PACKING, UNITS_PER_HOUR, 0, List.of(
            new HeadcountProductivityDataRequest(A_DATE_UTC, 92),
            new HeadcountProductivityDataRequest(A_DATE_UTC.plusHours(1), 85)
        ))
    );
  }

  private static List<HeadcountDistributionRequest> mockHeadcounts() {
    return singletonList(
        new HeadcountDistributionRequest(PICKING, PERCENTAGE, mockPickingAreas())
    );
  }

  private static List<AreaRequest> mockPickingAreas() {
    return asList(
        new AreaRequest("MZ", 85),
        new AreaRequest("RS", 5),
        new AreaRequest("HV", 5),
        new AreaRequest("BL", 5)
    );
  }

  public static List<EntityOutput> mockGetRemainingProcessingOutput() {
    return List.of(
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .metricUnit(UNITS)
            .processName(WAVING)
            .source(FORECAST)
            .type(REMAINING_PROCESSING)
            .value(1500)
            .workflow(FBM_WMS_OUTBOUND)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .metricUnit(UNITS)
            .processName(WAVING)
            .source(FORECAST)
            .type(REMAINING_PROCESSING)
            .value(1500)
            .workflow(FBM_WMS_OUTBOUND)
            .build()
    );
  }

  public static List<EntityOutput> mockGetPerformedProcessingOutput() {
    return List.of(
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .metricUnit(UNITS)
            .processName(WAVING)
            .source(FORECAST)
            .type(PERFORMED_PROCESSING)
            .value(1500)
            .workflow(FBM_WMS_OUTBOUND)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .metricUnit(UNITS)
            .processName(WAVING)
            .source(FORECAST)
            .type(PERFORMED_PROCESSING)
            .value(1500)
            .workflow(FBM_WMS_OUTBOUND)
            .build()
    );
  }

  public static List<EntityOutput> mockSearchBacklogLowerLimitOutput() {
    return List.of(
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .metricUnit(MINUTES)
            .processName(WAVING)
            .source(FORECAST)
            .type(BACKLOG_LOWER_LIMIT)
            .value(0)
            .workflow(FBM_WMS_OUTBOUND)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .metricUnit(MINUTES)
            .processName(WAVING)
            .source(FORECAST)
            .type(BACKLOG_LOWER_LIMIT)
            .value(15)
            .workflow(FBM_WMS_OUTBOUND)
            .build()
    );
  }

  public static List<EntityOutput> mockSearchBacklogUpperLimitOutput() {
    return List.of(
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .metricUnit(MINUTES)
            .processName(WAVING)
            .source(FORECAST)
            .type(BACKLOG_UPPER_LIMIT)
            .value(360)
            .workflow(FBM_WMS_OUTBOUND)
            .build(),
        EntityOutput.builder()
            .date(A_DATE_UTC)
            .metricUnit(MINUTES)
            .processName(WAVING)
            .source(FORECAST)
            .type(BACKLOG_UPPER_LIMIT)
            .value(180)
            .workflow(FBM_WMS_OUTBOUND)
            .build()
    );
  }

  public static List<MaxCapacityOutput> getMockOutputCapacities() {
    return List.of(
        new MaxCapacityOutput(WAREHOUSE_ID, A_DATE_UTC, A_DATE_UTC, 300),
        new MaxCapacityOutput(WAREHOUSE_ID, A_DATE_UTC, A_DATE_UTC, 350),
        new MaxCapacityOutput(WAREHOUSE_ID, A_DATE_UTC, A_DATE_UTC, 400));
  }

  public static List<MaxCapacityView> getMockEntityCapacities() {

    return List.of(
        getMockView(WAREHOUSE_ID, A_DATE_UTC, A_DATE_UTC, 300),
        getMockView(WAREHOUSE_ID, A_DATE_UTC, A_DATE_UTC, 350),
        getMockView(WAREHOUSE_ID, A_DATE_UTC, A_DATE_UTC, 400));
  }

  private static MaxCapacityView getMockView(final String warehouse,
                                             final ZonedDateTime dateFrom,
                                             final ZonedDateTime dateTo,
                                             final long quantity) {

    final MaxCapacityView maxCapacityView = mock(MaxCapacityView.class);

    when(maxCapacityView.getLogisticCenterId()).thenReturn(warehouse);
    when(maxCapacityView.getLoadDate()).thenReturn(Date.from(dateFrom.toInstant()));
    when(maxCapacityView.getMaxCapacityDate()).thenReturn(Date.from(dateTo.toInstant()));
    when(maxCapacityView.getMaxCapacityValue()).thenReturn(quantity);

    return maxCapacityView;
  }

  public static Map<EntityType, Object> mockSearchEntitiesOutput() {
    return Map.of(
        HEADCOUNT, mockHeadcountEntityOutput(),
        PRODUCTIVITY, mockProductivityEntityOutput(),
        EntityType.REMAINING_PROCESSING, mockGetRemainingProcessingOutput(),
        THROUGHPUT, mockThroughputEntityOutput()
    );
  }

  public static CurrentForecastDeviation mockCurrentForecastDeviation() {
    return CurrentForecastDeviation
        .builder()
        .workflow(FBM_WMS_OUTBOUND)
        .dateFrom(DATE_IN)
        .dateTo(DATE_OUT)
        .value(0.025)
        .userId(USER_ID)
        .logisticCenterId(WAREHOUSE_ID)
        .isActive(true)
        .build();
  }

  public static CurrentForecastDeviation mockCurrentForecastDeviation(final ZonedDateTime dateIn,
                                                                      final ZonedDateTime dateTo,
                                                                      final boolean isActive) {
    return CurrentForecastDeviation
        .builder()
        .workflow(FBM_WMS_OUTBOUND)
        .dateFrom(dateIn)
        .dateTo(dateTo)
        .value(0.025)
        .userId(USER_ID)
        .logisticCenterId(WAREHOUSE_ID)
        .isActive(isActive)
        .build();
  }

  public static CurrentForecastDeviation mockCurrentForecastDeviationWithPath(final Path path) {
    return CurrentForecastDeviation.builder()
        .logisticCenterId(WAREHOUSE_ID)
        .workflow(FBM_WMS_INBOUND)
        .isActive(TRUE)
        .value(0.1)
        .path(path)
        .type(DeviationType.UNITS)
        .dateFrom(A_DATE_UTC)
        .dateTo(A_DATE_UTC.plus(1, DAYS))
        .dateCreated(A_DATE_UTC)
        .lastUpdated(A_DATE_UTC)
        .userId(131206L)
        .build();
  }

  public static List<CurrentForecastDeviation> mockListOfCurrentForecastDeviations() {
    return List.of(
        CurrentForecastDeviation
            .builder()
            .workflow(FBM_WMS_INBOUND)
            .dateFrom(DATE_IN)
            .dateTo(DATE_OUT)
            .value(0.025)
            .userId(USER_ID)
            .logisticCenterId(WAREHOUSE_ID)
            .isActive(true)
            .type(DeviationType.MINUTES)
            .build(),
        CurrentForecastDeviation
            .builder()
            .workflow(FBM_WMS_INBOUND)
            .dateFrom(DATE_IN)
            .dateTo(DATE_OUT)
            .value(0.025)
            .userId(USER_ID)
            .type(DeviationType.UNITS)
            .logisticCenterId(WAREHOUSE_ID)
            .isActive(true)
            .build()
    );
  }

  public static List<Long> mockForecastIds() {
    return List.of(1L, 2L);
  }

  public static Map<InputId, Object> mockInputsCatalog() {
    final Map<InputId, Object> inputResult = new LinkedHashMap<>();
    inputResult.put(ABSENCES, List.of());
    inputResult.put(BACKLOG_BOUNDS, List.of());
    inputResult.put(CONFIGURATION, null);
    inputResult.put(CONTRACT_MODALITY_TYPES, List.of());
    inputResult.put(NON_SYSTEMIC_RATIO, List.of());
    inputResult.put(POLYVALENCE_PARAMETERS, List.of());
    inputResult.put(PRESENCES, List.of());
    inputResult.put(SHIFT_CONTRACT_MODALITIES, List.of());
    inputResult.put(SHIFTS_PARAMETERS, List.of());
    inputResult.put(TRANSFERS, List.of());
    inputResult.put(WORKERS_COSTS, List.of());
    inputResult.put(WORKERS_PARAMETERS, List.of());
    return inputResult;
  }

}
