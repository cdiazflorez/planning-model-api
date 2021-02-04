package com.mercadolibre.planning.model.api.usecase;

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
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.HeadcountProductivityGateway;
import com.mercadolibre.planning.model.api.gateway.PlanningDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
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
    private ForecastGateway forecastGateway;

    @Mock
    private ProcessingDistributionGateway processingDistributionGateway;

    @Mock
    private HeadcountDistributionGateway headcountDistributionGateway;

    @Mock
    private HeadcountProductivityGateway headcountProductivityGateway;

    @Mock
    private PlanningDistributionGateway planningDistributionGateway;

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
                new ProcessingDistribution(0, DATE_IN, WAVING,
                        172, UNITS, PERFORMED_PROCESSING, forecast),
                new ProcessingDistribution(0, DATE_IN.plusHours(1), WAVING,
                        295, UNITS, PERFORMED_PROCESSING, forecast)
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
                0, DATE_IN, DATE_OUT, 1200, UNITS, forecast, getPlanningMetadatas())
        );
    }

    private List<PlanningDistributionMetadata> getPlanningMetadatas() {
        return List.of(
                new PlanningDistributionMetadata(0, "carrier_id", "17502740"),
                new PlanningDistributionMetadata(0, "service_id", "851"),
                new PlanningDistributionMetadata(0, "canalization", "U")
        );
    }

    private List<HeadcountProductivity> getAllProductivities(final Forecast forecast) {
        return List.of(
                new HeadcountProductivity(0, A_DATE_UTC, PICKING, 85, UNITS_PER_HOUR,
                        0, forecast),
                new HeadcountProductivity(0, A_DATE_UTC, PICKING, 73, UNITS_PER_HOUR,
                        1, forecast),
                new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), PICKING, 85, UNITS_PER_HOUR,
                        0, forecast),
                new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), PICKING, 73, UNITS_PER_HOUR,
                        1, forecast),
                new HeadcountProductivity(0, A_DATE_UTC, PACKING, 92, UNITS_PER_HOUR,
                        0, forecast),
                new HeadcountProductivity(0, A_DATE_UTC, PACKING, 82, UNITS_PER_HOUR,
                        1, forecast),
                new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), PACKING, 85, UNITS_PER_HOUR,
                        0, forecast),
                new HeadcountProductivity(0, A_DATE_UTC.plusHours(1), PACKING, 76, UNITS_PER_HOUR,
                        1, forecast)

        );
    }
}
