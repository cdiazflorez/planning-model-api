package com.mercadolibre.planning.model.api.util;

import com.mercadolibre.planning.model.api.dao.current.CurrentHeadcountProductivityDao;
import com.mercadolibre.planning.model.api.dao.current.CurrentProcessingDistributionDao;
import com.mercadolibre.planning.model.api.dao.forecast.ForecastDao;
import com.mercadolibre.planning.model.api.dao.forecast.ForecastMetadataDao;
import com.mercadolibre.planning.model.api.dao.forecast.HeadcountDistributionDao;
import com.mercadolibre.planning.model.api.dao.forecast.HeadcountProductivityDao;
import com.mercadolibre.planning.model.api.dao.forecast.PlanningDistributionDao;
import com.mercadolibre.planning.model.api.dao.forecast.PlanningDistributionMetadataDao;
import com.mercadolibre.planning.model.api.dao.forecast.ProcessingDistributionDao;
import com.mercadolibre.planning.model.api.domain.ProcessName;

import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.MetricUnit.UNIT;
import static com.mercadolibre.planning.model.api.domain.MetricUnit.UNIT_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.MetricUnit.WORKER;
import static com.mercadolibre.planning.model.api.domain.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.ProcessingType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.Workflow.FBM_WMS_OUTBOUND;
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

    public static ForecastDao mockForecastDao(final Set<HeadcountDistributionDao> headcountDists,
                                              final Set<HeadcountProductivityDao> productivities,
                                              final Set<PlanningDistributionDao> planningDists,
                                              final Set<ProcessingDistributionDao> procDists,
                                              final Set<ForecastMetadataDao> forecastMetadatas) {
        return forecastDaoBuilder()
                .headcountDistributions(headcountDists)
                .processingDistributions(procDists)
                .headcountProductivities(productivities)
                .planningDistributions(planningDists)
                .metadatas(forecastMetadatas)
                .build();
    }

    public static ForecastDao mockSimpleForecastDao() {
        return forecastDaoBuilder().build();
    }

    public static ForecastDao.ForecastDaoBuilder forecastDaoBuilder() {
        return ForecastDao.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .dateCreated(A_DATE)
                .lastUpdated(A_DATE);
    }

    public static PlanningDistributionDao mockPlanningDistribution(final ForecastDao forecastDao) {
        return planningDistBuilder().forecast(forecastDao).build();
    }

    public static PlanningDistributionDao.PlanningDistributionDaoBuilder planningDistBuilder() {
        return PlanningDistributionDao.builder()
                .dateIn(DATE_IN)
                .dateOut(DATE_OUT)
                .quantity(1200)
                .metadatas(emptySet())
                .quantityMetricUnit(UNIT);
    }

    public static HeadcountDistributionDao.HeadcountDistributionDaoBuilder headcountDistBuilder() {
        return HeadcountDistributionDao.builder()
                .area("MZ")
                .processName(PICKING)
                .quantity(40)
                .quantityMetricUnit(WORKER);
    }

    public static HeadcountDistributionDao mockHeadcountDistDao(final ForecastDao forecastDao) {
        return headcountDistBuilder().forecast(forecastDao).build();
    }

    public static HeadcountProductivityDao.HeadcountProductivityDaoBuilder headcountProdBuilder() {
        return HeadcountProductivityDao.builder()
                .abilityLevel(1L)
                .productivity(80)
                .productivityMetricUnit(PERCENTAGE)
                .processName(PACKING)
                .dayTime(AN_OFFSET_TIME);
    }

    public static HeadcountProductivityDao mockHeadcountProdDao(final ForecastDao forecastDao) {
        return headcountProdBuilder().forecast(forecastDao).build();
    }

    public static ProcessingDistributionDao.ProcessingDistributionDaoBuilder processDistBuilder() {
        return ProcessingDistributionDao.builder()
                .processName(ProcessName.WAVING)
                .quantity(1000)
                .quantityMetricUnit(UNIT)
                .type(REMAINING_PROCESSING)
                .date(A_DATE);
    }

    public static ProcessingDistributionDao mockProcessingDistDao(final ForecastDao forecastDao) {
        return processDistBuilder().forecast(forecastDao).build();
    }

    public static ForecastMetadataDao.ForecastMetadataDaoBuilder forecastMetadataBuilder() {
        return ForecastMetadataDao.builder()
                .key(FORECAST_METADATA_KEY)
                .value(FORECAST_METADATA_VALUE);
    }

    public static ForecastMetadataDao mockForecastMetadataDao(final ForecastDao forecastDao) {
        if (forecastDao == null) {
            return forecastMetadataBuilder().build();
        }

        return forecastMetadataBuilder().forecastId(forecastDao.getId()).build();
    }

    public static PlanningDistributionMetadataDao
            .PlanningDistributionMetadataDaoBuilder planningMetadataBuilder() {

        return PlanningDistributionMetadataDao.builder()
                .key(PLANNING_METADATA_KEY)
                .value(PLANNING_METADATA_VALUE);
    }

    public static PlanningDistributionMetadataDao mockPlanningDistMetadataDao(
            final PlanningDistributionDao planningDistDao) {

        if (planningDistDao == null) {
            return planningMetadataBuilder().build();
        }

        return planningMetadataBuilder().planningDistributionId(planningDistDao.getId()).build();
    }

    public static CurrentHeadcountProductivityDao mockCurrentProdDao() {
        return CurrentHeadcountProductivityDao.builder()
                .abilityLevel(1L)
                .date(A_DATE)
                .isActive(true)
                .productivity(68)
                .productivityMetricUnit(UNIT_PER_HOUR)
                .processName(PICKING)
                .workflow(FBM_WMS_OUTBOUND)
                .build();
    }

    public static CurrentProcessingDistributionDao mockCurrentProcDistDao() {
        return CurrentProcessingDistributionDao.builder()
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
