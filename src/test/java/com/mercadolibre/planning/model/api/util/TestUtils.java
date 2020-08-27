package com.mercadolibre.planning.model.api.util;

import com.mercadolibre.planning.model.api.web.controller.request.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivityEntity;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistributionEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadataEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistributionEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivityEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadataEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistributionEntity;

import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Set;

import static com.mercadolibre.planning.model.api.web.controller.request.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.web.controller.request.MetricUnit.UNIT;
import static com.mercadolibre.planning.model.api.web.controller.request.MetricUnit.UNIT_PER_HOUR;
import static com.mercadolibre.planning.model.api.web.controller.request.MetricUnit.WORKER;
import static com.mercadolibre.planning.model.api.web.controller.request.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.web.controller.request.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.web.controller.request.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.web.controller.request.ProcessingType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.web.controller.request.Workflow.FBM_WMS_OUTBOUND;
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

    public static ForecastEntity mockForecast(final Set<HeadcountDistributionEntity> headcountDists,
                                              final Set<HeadcountProductivityEntity> productivities,
                                              final Set<PlanningDistributionEntity> planningDists,
                                              final Set<ProcessingDistributionEntity> procDists,
                                              final Set<ForecastMetadataEntity> forecastMetadatas) {
        return forecastEntityBuilder()
                .headcountDistributions(headcountDists)
                .processingDistributions(procDists)
                .headcountProductivities(productivities)
                .planningDistributions(planningDists)
                .metadatas(forecastMetadatas)
                .build();
    }

    public static ForecastEntity mockSimpleForecast() {
        return forecastEntityBuilder().build();
    }

    public static ForecastEntity.ForecastEntityBuilder forecastEntityBuilder() {
        return ForecastEntity.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .dateCreated(A_DATE)
                .lastUpdated(A_DATE);
    }

    public static PlanningDistributionEntity mockPlanningDist(final ForecastEntity forecast) {
        return planningBuilder().forecast(forecast).build();
    }

    public static PlanningDistributionEntity.PlanningDistributionEntityBuilder planningBuilder() {
        return PlanningDistributionEntity.builder()
                .dateIn(DATE_IN)
                .dateOut(DATE_OUT)
                .quantity(1200)
                .metadatas(emptySet())
                .quantityMetricUnit(UNIT);
    }

    public static HeadcountDistributionEntity.HeadcountDistEntityBuilder headcountDistBuilder() {
        return HeadcountDistributionEntity.builder()
                .area("MZ")
                .processName(PICKING)
                .quantity(40)
                .quantityMetricUnit(WORKER);
    }

    public static HeadcountDistributionEntity mockHeadcountDist(final ForecastEntity forecast) {

        return headcountDistBuilder().forecast(forecast).build();
    }

    public static HeadcountProductivityEntity.HeadcountProdBuilder headcountProdBuilder() {
        return HeadcountProductivityEntity.builder()
                .abilityLevel(1L)
                .productivity(80)
                .productivityMetricUnit(PERCENTAGE)
                .processName(PACKING)
                .dayTime(AN_OFFSET_TIME);
    }

    public static HeadcountProductivityEntity mockHeadcountProd(final ForecastEntity forecast) {
        return headcountProdBuilder().forecast(forecast).build();
    }

    public static ProcessingDistributionEntity.ProcessingDistBuilder processDistBuilder() {
        return ProcessingDistributionEntity.builder()
                .processName(ProcessName.WAVING)
                .quantity(1000)
                .quantityMetricUnit(UNIT)
                .type(REMAINING_PROCESSING)
                .date(A_DATE);
    }

    public static ProcessingDistributionEntity mockProcessingDist(final ForecastEntity forecast) {
        return processDistBuilder().forecast(forecast).build();
    }

    public static ForecastMetadataEntity.ForecastMetadataEntityBuilder forecastMetadataBuilder() {
        return ForecastMetadataEntity.builder()
                .key(FORECAST_METADATA_KEY)
                .value(FORECAST_METADATA_VALUE);
    }

    public static ForecastMetadataEntity mockForecastMetadata(final ForecastEntity forecast) {
        if (forecast == null) {
            return forecastMetadataBuilder().build();
        }

        return forecastMetadataBuilder().forecastId(forecast.getId()).build();
    }

    public static PlanningDistributionMetadataEntity
            .PlanningDistributionMetadataEntityBuilder planningMetadataBuilder() {

        return PlanningDistributionMetadataEntity.builder()
                .key(PLANNING_METADATA_KEY)
                .value(PLANNING_METADATA_VALUE);
    }

    public static PlanningDistributionMetadataEntity mockPlanningDistMetadata(
            final PlanningDistributionEntity planningDistEntity) {

        if (planningDistEntity == null) {
            return planningMetadataBuilder().build();
        }

        return planningMetadataBuilder()
                .planningDistributionId(planningDistEntity.getId())
                .build();
    }

    public static CurrentHeadcountProductivityEntity mockCurrentProdEntity() {
        return CurrentHeadcountProductivityEntity.builder()
                .abilityLevel(1L)
                .date(A_DATE)
                .isActive(true)
                .productivity(68)
                .productivityMetricUnit(UNIT_PER_HOUR)
                .processName(PICKING)
                .workflow(FBM_WMS_OUTBOUND)
                .build();
    }

    public static CurrentProcessingDistributionEntity mockCurrentProcDist() {
        return CurrentProcessingDistributionEntity.builder()
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
