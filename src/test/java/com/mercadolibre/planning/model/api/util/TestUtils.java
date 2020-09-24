package com.mercadolibre.planning.model.api.util;

import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.input.CreateForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetEntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.AreaRequest;
import com.mercadolibre.planning.model.api.web.controller.request.HeadcountDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.HeadcountProductivityDataRequest;
import com.mercadolibre.planning.model.api.web.controller.request.HeadcountProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.MetadataRequest;
import com.mercadolibre.planning.model.api.web.controller.request.PlanningDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.PolyvalentProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.ProcessingDistributionDataRequest;
import com.mercadolibre.planning.model.api.web.controller.request.ProcessingDistributionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_TO_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

@SuppressWarnings("PMD.ExcessiveImports")
public final class TestUtils {

    public static final ZonedDateTime A_DATE_UTC = ZonedDateTime.of(2020, 8, 19, 17, 0, 0, 0,
            ZoneId.of("UTC"));
    public static final ZonedDateTime A_DATE = ZonedDateTime.of(2020, 8, 19, 14, 0, 0, 0,
            ZoneId.of("-3"));
    public static final ZonedDateTime DATE_IN = ZonedDateTime.of(2020, 8, 19, 18, 0, 0, 0,
            ZoneId.of("UTC"));
    public static final ZonedDateTime DATE_OUT = ZonedDateTime.of(2020, 8, 20, 15, 30, 0, 0,
            ZoneId.of("UTC"));
    public static final OffsetTime AN_OFFSET_TIME = OffsetTime.parse("00:00:00-03:00");
    public static final OffsetTime ANOTHER_OFFSET_TIME = OffsetTime.parse("01:00:00-03:00");
    public static final OffsetTime AN_OFFSET_TIME_UTC = AN_OFFSET_TIME.withOffsetSameInstant(UTC);
    public static final OffsetTime ANOTHER_OFFSET_TIME_UTC = ANOTHER_OFFSET_TIME
            .withOffsetSameInstant(UTC);
    public static final String FORECAST_METADATA_KEY = "mono_order_distribution";
    public static final String FORECAST_METADATA_VALUE = "48";
    public static final String PLANNING_METADATA_KEY = "carrier";
    public static final String PLANNING_METADATA_VALUE = "Mercado envíos";

    public static Forecast mockForecast(final Set<HeadcountDistribution> headcountDists,
                                        final Set<HeadcountProductivity> productivities,
                                        final Set<PlanningDistribution> planningDists,
                                        final Set<ProcessingDistribution> procDists,
                                        final Set<ForecastMetadata> forecastMetadatas) {
        return forecastEntityBuilder()
                .headcountDistributions(headcountDists)
                .processingDistributions(procDists)
                .headcountProductivities(productivities)
                .planningDistributions(planningDists)
                .metadatas(forecastMetadatas)
                .build();
    }

    public static Forecast mockSimpleForecast() {
        return forecastEntityBuilder().build();
    }

    public static Forecast.ForecastBuilder forecastEntityBuilder() {
        return Forecast.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .dateCreated(A_DATE_UTC)
                .lastUpdated(A_DATE_UTC);
    }

    public static PlanningDistribution mockPlanningDist(final Forecast forecast) {
        return planningBuilder().forecast(forecast).build();
    }

    public static PlanningDistribution.PlanningDistributionBuilder planningBuilder() {
        return PlanningDistribution.builder()
                .dateIn(DATE_IN)
                .dateOut(DATE_OUT)
                .quantity(1200)
                .metadatas(emptySet())
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
                .dayTime(AN_OFFSET_TIME);
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

    public static CurrentHeadcountProductivity mockCurrentProdEntity() {
        return CurrentHeadcountProductivity.builder()
                .abilityLevel(1L)
                .date(A_DATE_UTC)
                .isActive(true)
                .productivity(68)
                .productivityMetricUnit(UNITS_PER_HOUR)
                .processName(PICKING)
                .workflow(FBM_WMS_OUTBOUND)
                .build();
    }

    public static CurrentProcessingDistribution mockCurrentProcDist() {
        return CurrentProcessingDistribution.builder()
                .date(A_DATE_UTC)
                .isActive(false)
                .processName(PACKING)
                .quantity(35)
                .quantityMetricUnit(WORKERS)
                .type(ACTIVE_WORKERS)
                .workflow(FBM_WMS_OUTBOUND)
                .build();
    }

    public static CreateForecastInput mockCreateForecastInput() {
        return CreateForecastInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .headcountDistributions(mockHeadcounts())
                .headcountProductivities(mockProductivities())
                .planningDistributions(mockPlanningDistributions())
                .processingDistributions(mockProcessingDistributions())
                .polyvalentProductivities(mockPolyvalentProductivities())
                .metadata(mockMetadatas())
                .build();
    }

    public static GetEntityInput mockGetHeadcountEntityInput(final Source source) {
        return new GetEntityInput("ARBA01", FBM_WMS_OUTBOUND, HEADCOUNT,
                A_DATE_UTC, A_DATE_UTC.plusDays(2), source);
    }

    public static List<GetEntityOutput> mockGetHeadcountEntityOutput() {
        return List.of(
                new GetEntityOutput(FBM_WMS_OUTBOUND, A_DATE_UTC, PICKING,
                        50, WORKERS, FORECAST),
                new GetEntityOutput(FBM_WMS_OUTBOUND, A_DATE_UTC.plusHours(1), PICKING,
                        40, WORKERS, FORECAST)
        );
    }

    public static String getResourceAsString(final String resourceName) throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final InputStream resource = classLoader.getResourceAsStream(resourceName);

        try {
            return IOUtils.toString(resource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            resource.close();
        }
    }

    private static List<ProcessingDistributionRequest> mockProcessingDistributions() {
        final List<ProcessingDistributionDataRequest> data = asList(
                new ProcessingDistributionDataRequest(DATE_IN, 172),
                new ProcessingDistributionDataRequest(DATE_IN.plusHours(1), 295)
        );

        return singletonList(
                new ProcessingDistributionRequest(PERFORMED_PROCESSING, UNITS, WAVING, data)
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
                        DATE_IN, DATE_OUT, UNITS, 1200, asList(
                        new MetadataRequest("carrier_id", "17502740"),
                        new MetadataRequest("service_id", "851"),
                        new MetadataRequest("canalization", "U")
                ))
        );
    }

    private static List<MetadataRequest> mockMetadatas() {
        return asList(
                new MetadataRequest("warehouse_id", "ARBA01"),
                new MetadataRequest("week", "26-2020"),
                new MetadataRequest("mono_order_distribution", "58"),
                new MetadataRequest("multi_order_distribution", "42")
        );
    }

    private static List<HeadcountProductivityRequest> mockProductivities() {
        return asList(
                new HeadcountProductivityRequest(PICKING, UNITS_PER_HOUR, 0, List.of(
                        new HeadcountProductivityDataRequest(AN_OFFSET_TIME, 85),
                        new HeadcountProductivityDataRequest(ANOTHER_OFFSET_TIME, 85)
                )),
                new HeadcountProductivityRequest(PACKING, UNITS_PER_HOUR, 0, List.of(
                        new HeadcountProductivityDataRequest(AN_OFFSET_TIME, 92),
                        new HeadcountProductivityDataRequest(ANOTHER_OFFSET_TIME, 85)
                ))
        );
    }

    private static List<HeadcountDistributionRequest> mockHeadcounts() {
        return asList(
                new HeadcountDistributionRequest(PICKING, PERCENTAGE, mockPickingAreas()),
                new HeadcountDistributionRequest(PUT_TO_WALL, PERCENTAGE, mockPtwAreas())
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

    private static List<AreaRequest> mockPtwAreas() {
        return asList(
                new AreaRequest("IN", 60),
                new AreaRequest("OUT", 40)
        );
    }
}
