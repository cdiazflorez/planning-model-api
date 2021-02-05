package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.SuggestedWavePlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.remainingprocessing.get.GetRemainingProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.GetSuggestedWavesInput;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.GetSuggestedWavesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.SuggestedWavesOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.HOUR_IN_MINUTES;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastByWarehouseId;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetSuggestedWavesInput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetSuggestedWavesUseCaseTest {

    @Mock
    private PlanningDistributionRepository planningDistRepository;

    @Mock
    private GetForecastMetadataUseCase getForecastMetadataUseCase;

    @Mock
    private GetRemainingProcessingUseCase getRemainingProcessingUseCase;

    @InjectMocks
    private GetSuggestedWavesUseCase getSuggestedWavesUseCase;

    @Test
    @DisplayName("Get suggested waves by cardinality")
    public void testGetSuggestedWavesOk() {
        // GIVEN
        final ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
        final GetSuggestedWavesInput input = mockGetSuggestedWavesInput(now);
        final Set<String> forecastWeeks = getForecastWeeks(input.getDateFrom(), input.getDateTo());

        when(planningDistRepository.findByWarehouseIdWorkflowDateInRange(
                input.getWarehouseId(),
                input.getWorkflow().name(),
                now.truncatedTo(HOURS),
                now.truncatedTo(HOURS).plusHours(1).minusMinutes(1),
                forecastWeeks)
        ).thenReturn(mockPlanningDistSuggestedWaveCurrent());

        when(planningDistRepository.findByWarehouseIdWorkflowDateInRange(
                input.getWarehouseId(),
                input.getWorkflow().name(),
                now.plusHours(1).truncatedTo(HOURS),
                input.getDateTo().minusMinutes(1),
                forecastWeeks)
        ).thenReturn(mockPlanningDistSuggestedWaveNext());

        when(getRemainingProcessingUseCase.execute(
                GetEntityInput.builder()
                        .workflow(input.getWorkflow())
                        .processName(List.of(PICKING))
                        .warehouseId(input.getWarehouseId())
                        .entityType(REMAINING_PROCESSING)
                        .dateFrom(input.getDateTo().minusHours(1))
                        .dateTo(input.getDateTo().minusHours(1))
                        .build())
        ).thenReturn(mockRemainingProcessing());

        when(getForecastMetadataUseCase.execute(GetForecastMetadataInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(mockForecastByWarehouseId());

        // WHEN
        final List<SuggestedWavesOutput> output = getSuggestedWavesUseCase.execute(input);

        // THEN
        suggestedWavesOutputEqualTo(output.get(0),
                WaveCardinality.MONO_ORDER_DISTRIBUTION, 100L);
        suggestedWavesOutputEqualTo(output.get(1),
                WaveCardinality.MULTI_ORDER_DISTRIBUTION, 100L);
        suggestedWavesOutputEqualTo(output.get(2),
                WaveCardinality.MULTI_BATCH_DISTRIBUTION, 300L);
    }

    private void suggestedWavesOutputEqualTo(final SuggestedWavesOutput output,
                                         final WaveCardinality waveCardinality,
                                         final float quantity) {
        assertEquals(waveCardinality, output.getWaveCardinality());
        assertTrue(output.getQuantity() > quantity);
    }

    private SuggestedWavePlanningDistributionView mockPlanningDistSuggestedWaveCurrent() {
        final ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
        return new SuggestedWavePlanningDistributionViewImpl(
                100L * (HOUR_IN_MINUTES - now.withFixedOffsetZone().getMinute())
                        / HOUR_IN_MINUTES);
    }

    private SuggestedWavePlanningDistributionView mockPlanningDistSuggestedWaveNext() {
        return new SuggestedWavePlanningDistributionViewImpl(160L);
    }

    private List<EntityOutput> mockRemainingProcessing() {
        return List.of(
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .value(89L)
                        .source(SIMULATION)
                        .processName(PICKING)
                        .metricUnit(UNITS)
                        .type(ProcessingType.REMAINING_PROCESSING)
                        .build()
        );
    }

    @Test
    @DisplayName("Get suggested waves by cardinality Empty")
    public void testGetSuggestedWavesEmpty() {
        // GIVEN
        final ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
        final GetSuggestedWavesInput input = mockGetSuggestedWavesInput(now);
        final Set<String> forecastWeeks = getForecastWeeks(input.getDateFrom(), input.getDateTo());

        when(planningDistRepository.findByWarehouseIdWorkflowDateInRange(
                input.getWarehouseId(),
                input.getWorkflow().name(),
                now.truncatedTo(HOURS),
                now.truncatedTo(HOURS).plusHours(1).minusMinutes(1),
                forecastWeeks)
        ).thenReturn(null);

        when(planningDistRepository.findByWarehouseIdWorkflowDateInRange(
                input.getWarehouseId(),
                input.getWorkflow().name(),
                now.plusHours(1).truncatedTo(HOURS),
                input.getDateTo().minusMinutes(1),
                forecastWeeks)
        ).thenReturn(null);

        when(getRemainingProcessingUseCase.execute(
                GetEntityInput.builder()
                        .workflow(input.getWorkflow())
                        .processName(List.of(PICKING))
                        .warehouseId(input.getWarehouseId())
                        .entityType(REMAINING_PROCESSING)
                        .dateFrom(input.getDateTo().minusHours(1))
                        .dateTo(input.getDateTo().minusHours(1))
                        .build())
        ).thenReturn(List.of());

        when(getForecastMetadataUseCase.execute(GetForecastMetadataInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(List.of());

        // WHEN
        final List<SuggestedWavesOutput> output = getSuggestedWavesUseCase.execute(input);
        // THEN
        assertEquals(List.of(), output);
    }

}
