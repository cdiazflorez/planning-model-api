package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.search.SearchEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionElemView;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionViewImpl;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.GetSuggestedWavesInput;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.GetSuggestedWavesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.SuggestedWavesOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.HOUR_IN_MINUTES;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastByWarehouseId;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastIds;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetSuggestedWavesInput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetSuggestedWavesUseCaseTest {

    @Mock
    private PlanningDistributionService planningDistributionService;

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private GetForecastMetadataUseCase getForecastMetadataUseCase;

    @Mock
    private SearchEntityUseCase searchEntityUseCase;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @InjectMocks
    private GetSuggestedWavesUseCase getSuggestedWavesUseCase;

    @Test
    @DisplayName("Get suggested waves by cardinality")
    public void testGetSuggestedWavesOk() {
        // GIVEN
        final GetSuggestedWavesInput input = mockGetSuggestedWavesInput();
        final List<Long> forecastIds = mockForecastIds();
        final long firstHourSales = 100L;
        final long nextHourSales = 200L;
        final long remainingProcessing = 308L;
        final long capex = 1000L;
        final List<PlanningDistributionElemView> planningDistribution = List.of(
                new PlanningDistributionViewImpl(1, null, null, firstHourSales, null)
        );

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(forecastIds);

        when(planningDistributionService.getPlanningDistribution(
                null,
                null,
                input.getDateFrom().truncatedTo(HOURS),
                input.getDateFrom().truncatedTo(HOURS).plusHours(1).minusMinutes(1),
                mockForecastIds()
            )
        ).thenReturn(planningDistribution);

        when(planningDistributionService.applyDeviation(anyString(), any()))
                .thenAnswer(answer -> answer.getArgument(1));

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                Set.of(MAX_CAPACITY.name()),
                List.of(GLOBAL.toJson()),
                input.getDateTo().minusHours(1),
                input.getDateTo().minusHours(1),
                forecastIds)
        ).thenReturn(mockCapexResponse(input, capex));

        when(planningDistributionService.getPlanningDistribution(
                        null,
                        null,
                        input.getDateFrom().truncatedTo(HOURS).plusHours(1),
                        input.getDateTo().minusMinutes(1),
                        mockForecastIds()
                )
        ).thenReturn(List.of(new PlanningDistributionViewImpl(1, null, null, nextHourSales, null)));

        when(searchEntityUseCase.execute(
                GetEntityInput.builder()
                        .workflow(input.getWorkflow())
                        .processName(List.of(PICKING))
                        .warehouseId(input.getWarehouseId())
                        .entityType(REMAINING_PROCESSING)
                        .dateFrom(input.getDateTo().minusHours(1))
                        .dateTo(input.getDateTo().minusHours(1))
                        .build())
        ).thenReturn(mockRemainingProcessing(remainingProcessing));

        when(getForecastMetadataUseCase.execute(GetForecastMetadataInput.builder()
                .forecastIds(mockForecastIds())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(mockForecastByWarehouseId());

        // WHEN
        final List<SuggestedWavesOutput> output = getSuggestedWavesUseCase.execute(input);

        // THEN
        final long totalSales = firstHourSales
                * (HOUR_IN_MINUTES - input.getDateFrom().withFixedOffsetZone().getMinute())
                / HOUR_IN_MINUTES + nextHourSales;
        final long suggestedWavingUnits = (long) Math.floor(
                (input.getBacklog() + totalSales)
                        / (1 + ((double) remainingProcessing / HOUR_IN_MINUTES))
        );
        final long unitsToWave =
                Math.min(suggestedWavingUnits, Math.min(capex, input.getBacklog()));

        suggestedWavesOutputEqualTo(output.get(0),
                WaveCardinality.MONO_ORDER_DISTRIBUTION, Math.floor(unitsToWave * 20.0 / 100));
        suggestedWavesOutputEqualTo(output.get(1),
                WaveCardinality.MULTI_ORDER_DISTRIBUTION, Math.floor(unitsToWave * 20.0 / 100));
        suggestedWavesOutputEqualTo(output.get(2),
                WaveCardinality.MULTI_BATCH_DISTRIBUTION, Math.floor(unitsToWave * 60.0 / 100));
    }

    private List<ProcessingDistributionView> mockCapexResponse(final GetSuggestedWavesInput input,
                                                               final long quantity) {
        return List.of(new ProcessingDistributionViewImpl(
                1L,
                Date.from(input.getDateTo().minusHours(1).toInstant()),
                GLOBAL,
                quantity,
                UNITS_PER_HOUR,
                MAX_CAPACITY
        ));
    }

    private void suggestedWavesOutputEqualTo(final SuggestedWavesOutput output,
                                             final WaveCardinality waveCardinality,
                                             final double quantity) {
        assertEquals(waveCardinality, output.getWaveCardinality());
        assertEquals(quantity, output.getQuantity());
    }

    private List<EntityOutput> mockRemainingProcessing(final long value) {
        return List.of(
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .date(A_DATE_UTC)
                        .quantity(value)
                        .source(FORECAST)
                        .processName(PICKING)
                        .metricUnit(MINUTES)
                        .type(ProcessingType.REMAINING_PROCESSING)
                        .build()
        );
    }

    @Test
    @DisplayName("Get suggested waves by cardinality Empty")
    public void testGetSuggestedWavesEmpty() {
        // GIVEN
        final GetSuggestedWavesInput input = mockGetSuggestedWavesInput();
        final List<Long> forecastIds = mockForecastIds();

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(mockForecastIds());

        when(planningDistributionService.getPlanningDistribution(
                        null,
                        null,
                        input.getDateFrom().truncatedTo(HOURS),
                        input.getDateFrom().truncatedTo(HOURS).plusHours(1).minusMinutes(1),
                        forecastIds
                )
        ).thenReturn(List.of());

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                Set.of(MAX_CAPACITY.name()),
                List.of(GLOBAL.toJson()),
                input.getDateTo().minusHours(1),
                input.getDateTo().minusHours(1),
                forecastIds)
        ).thenReturn(List.of());

        when(searchEntityUseCase.execute(
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
                .forecastIds(forecastIds)
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
