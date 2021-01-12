package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningMetadataRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_TO_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PERFORMED_PROCESSING;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCreateForecastInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateForecastUseCaseTest {

    @Mock
    private ForecastRepository forecastRepository;

    @Mock
    private ForecastMetadataRepository forecastMetadataRepository;

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private HeadcountDistributionRepository headcountRepository;

    @Mock
    private HeadcountProductivityRepository productivityRepository;

    @Mock
    private PlanningDistributionRepository planningRepository;

    @Mock
    private PlanningMetadataRepository planningMetadataRepository;

    @InjectMocks
    private CreateForecastUseCase createForecastUseCase;

    @Test
    @DisplayName("A forecast is created successfully")
    public void createSaveOk() {
        // GIVEN
        final Forecast forecast = new Forecast();
        forecast.setWorkflow(Workflow.FBM_WMS_OUTBOUND);

        final Forecast savedForecast = new Forecast();
        savedForecast.setWorkflow(Workflow.FBM_WMS_OUTBOUND);
        savedForecast.setId(1L);

        when(forecastRepository.save(forecast)).thenReturn(savedForecast);

        final Set<ForecastMetadata> forecastMetadatas = getForecastMetadatas();
        final Set<ProcessingDistribution> processingDists = getProcessingDists(savedForecast);
        final Set<HeadcountDistribution> headcounts = getHeadcountDists(savedForecast);
        final Set<HeadcountProductivity> productivities = getAllProductivities(savedForecast);
        final Set<PlanningDistributionMetadata> planningMetadatas = getPlanningMetadatas();

        final PlanningDistribution planningDist = new PlanningDistribution(
                0, DATE_IN, DATE_OUT, 1200, UNITS, savedForecast, null);

        final PlanningDistribution expectedSavedPlanningDist = getSavedPlanningDist(planningDist);
        when(planningRepository.save(planningDist)).thenReturn(expectedSavedPlanningDist);

        final CreateForecastInput input = mockCreateForecastInput();

        // WHEN
        final CreateForecastOutput output = createForecastUseCase.execute(input);

        // THEN
        verify(forecastMetadataRepository).saveAll(forecastMetadatas);
        verify(processingDistRepository).saveAll(processingDists);
        verify(headcountRepository).saveAll(headcounts);
        verify(productivityRepository).saveAll(productivities);
        verify(planningMetadataRepository).saveAll(planningMetadatas);
        assertEquals(1L, output.getId());
    }

    private Set<ForecastMetadata> getForecastMetadatas() {
        return Set.of(
                new ForecastMetadata(1, "warehouse_id", "ARBA01"),
                new ForecastMetadata(1, "week", "26-2020"),
                new ForecastMetadata(1, "mono_order_distribution", "58"),
                new ForecastMetadata(1, "multi_order_distribution", "42")
        );
    }

    private Set<ProcessingDistribution> getProcessingDists(final Forecast forecast) {
        return Set.of(
                new ProcessingDistribution(0, DATE_IN, WAVING,
                        172, UNITS, PERFORMED_PROCESSING, forecast),
                new ProcessingDistribution(0, DATE_IN.plusHours(1), WAVING,
                        295, UNITS, PERFORMED_PROCESSING, forecast)
        );
    }

    private Set<HeadcountDistribution> getHeadcountDists(final Forecast forecast) {
        return Set.of(
                new HeadcountDistribution(0, "MZ", PICKING, 85, PERCENTAGE, forecast),
                new HeadcountDistribution(0, "RS", PICKING, 5, PERCENTAGE, forecast),
                new HeadcountDistribution(0, "HV", PICKING, 5, PERCENTAGE, forecast),
                new HeadcountDistribution(0, "BL", PICKING, 5, PERCENTAGE, forecast),
                new HeadcountDistribution(0, "IN", PUT_TO_WALL, 60, PERCENTAGE, forecast),
                new HeadcountDistribution(0, "OUT", PUT_TO_WALL, 40, PERCENTAGE, forecast)
        );
    }

    private Set<PlanningDistributionMetadata> getPlanningMetadatas() {
        return Set.of(
                new PlanningDistributionMetadata(1, "carrier_id", "17502740"),
                new PlanningDistributionMetadata(1, "service_id", "851"),
                new PlanningDistributionMetadata(1, "canalization", "U")
        );
    }

    private Set<HeadcountProductivity> getAllProductivities(final Forecast forecast) {
        return Set.of(
                new HeadcountProductivity(0, A_DATE_UTC, PICKING, 85, UNITS_PER_HOUR,
                        0, forecast),
                new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), PICKING, 85,
                        UNITS_PER_HOUR, 0, forecast),
                new HeadcountProductivity(0, A_DATE_UTC, PICKING, 73, UNITS_PER_HOUR,
                        1, forecast),
                new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), PICKING, 73,
                        UNITS_PER_HOUR, 1, forecast),
                new HeadcountProductivity(0, A_DATE_UTC, PACKING, 92, UNITS_PER_HOUR,
                        0, forecast),
                new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), PACKING, 85,
                        UNITS_PER_HOUR, 0, forecast),
                new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), PACKING, 76,
                        UNITS_PER_HOUR, 1, forecast),
                new HeadcountProductivity(0, A_DATE_UTC, PACKING, 82, UNITS_PER_HOUR,
                        1, forecast)
        );
    }

    private PlanningDistribution getSavedPlanningDist(final PlanningDistribution planningDist) {
        return PlanningDistribution.builder()
                .id(1L)
                .dateIn(planningDist.getDateIn())
                .dateOut(planningDist.getDateOut())
                .forecast(planningDist.getForecast())
                .metadatas(planningDist.getMetadatas())
                .quantity(planningDist.getQuantity())
                .quantityMetricUnit(planningDist.getQuantityMetricUnit())
                .build();
    }
}
