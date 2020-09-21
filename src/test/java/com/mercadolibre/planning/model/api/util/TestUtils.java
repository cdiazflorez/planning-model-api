package com.mercadolibre.planning.model.api.util;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;

import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNIT;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNIT_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.util.Collections.emptySet;

public final class TestUtils {

    public static final ZonedDateTime A_DATE = ZonedDateTime.parse("2020-08-19T17:00:00.000Z");
    public static final ZonedDateTime DATE_IN = ZonedDateTime.parse("2020-08-19T18:00:00.000Z");
    public static final ZonedDateTime DATE_OUT = ZonedDateTime.parse("2020-08-20T15:30:00.000Z");
    public static final OffsetTime AN_OFFSET_TIME = OffsetTime.parse("10:00:00-03:00");
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
                .dateCreated(A_DATE)
                .lastUpdated(A_DATE);
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
                .quantityMetricUnit(UNIT);
    }

    public static HeadcountDistribution.HeadcountDistributionBuilder headcountDistBuilder() {
        return HeadcountDistribution.builder()
                .area("MZ")
                .processName(PICKING)
                .quantity(40)
                .quantityMetricUnit(WORKER);
    }

    public static HeadcountDistribution mockHeadcountDist(final Forecast forecast) {

        return headcountDistBuilder().forecast(forecast).build();
    }

    public static HeadcountProductivity.HeadcountProductivityBuilder headcountProdBuilder() {
        return HeadcountProductivity.builder()
                .abilityLevel(1L)
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
                .processName(ProcessName.WAVING)
                .quantity(1000)
                .quantityMetricUnit(UNIT)
                .type(REMAINING_PROCESSING)
                .date(A_DATE);
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
                .date(A_DATE)
                .isActive(true)
                .productivity(68)
                .productivityMetricUnit(UNIT_PER_HOUR)
                .processName(PICKING)
                .workflow(FBM_WMS_OUTBOUND)
                .build();
    }

    public static CurrentProcessingDistribution mockCurrentProcDist() {
        return CurrentProcessingDistribution.builder()
                .date(A_DATE)
                .isActive(false)
                .processName(PACKING)
                .quantity(35)
                .quantityMetricUnit(WORKER)
                .type(ACTIVE_WORKERS)
                .workflow(FBM_WMS_OUTBOUND)
                .build();
    }
}
