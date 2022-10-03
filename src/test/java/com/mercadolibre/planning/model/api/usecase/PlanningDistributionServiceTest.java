package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.currentPlanningDistributions;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastIds;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDistributionInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.planningDistributions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.LongVariable")
public class PlanningDistributionServiceTest {

    @Mock
    private PlanningDistributionRepository planningDistRepository;

    @Mock
    private CurrentPlanningDistributionRepository currentPlanningDistRepository;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @Mock
    private CurrentForecastDeviationRepository currentForecastDeviationRepository;

    @InjectMocks
    private PlanningDistributionService planningDistributionService;

    @Test
    @DisplayName("Get planning distribution from forecast without date in to")
    public void testGetPlanningDistributionOk() {
        // GIVEN
        final GetPlanningDistributionInput input = mockPlanningDistributionInput(null, null);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateOutFrom())
                .dateTo(input.getDateOutTo())
                .build())
        ).thenReturn(mockForecastIds());

        when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
                A_DATE_UTC,
                A_DATE_UTC.plusDays(3),
                null,
                null,
                mockForecastIds())
        ).thenReturn(planningDistributions());

        // WHEN
        final List<GetPlanningDistributionOutput> output = planningDistributionService
                .getPlanningDistribution(input);

        // THEN
        final GetPlanningDistributionOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
        assertEquals(1000, output1.getTotal());
        assertEquals(UNITS, output1.getMetricUnit());

        final GetPlanningDistributionOutput output3 = output.get(2);
        assertEquals(A_DATE_UTC.toInstant(), output3.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output3.getDateOut().toInstant());
        assertEquals(1200, output3.getTotal());
        assertEquals(UNITS, output3.getMetricUnit());
    }

    @Test
    @DisplayName("Get planning distribution from forecast with date in to")
    public void testGetPlanningDistributionWithDateInToOk() {
        // GIVEN
        final ZonedDateTime dateInTo = A_DATE_UTC.minusDays(3);
        final GetPlanningDistributionInput input = mockPlanningDistributionInput(null, dateInTo);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateOutFrom())
                .dateTo(input.getDateOutTo())
                .build())
        ).thenReturn(mockForecastIds());

        when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
                A_DATE_UTC,
                A_DATE_UTC.plusDays(3),
                null,
                dateInTo,
                mockForecastIds())
        ).thenReturn(planningDistributions());

        // WHEN
        final List<GetPlanningDistributionOutput> output = planningDistributionService
                .getPlanningDistribution(input);

        // THEN
        final GetPlanningDistributionOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
        assertEquals(1000, output1.getTotal());
        assertEquals(UNITS, output1.getMetricUnit());

        final GetPlanningDistributionOutput output2 = output.get(1);
        assertEquals(A_DATE_UTC.toInstant(), output2.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output2.getDateOut().toInstant());
        assertEquals(300, output2.getTotal());
        assertEquals(UNITS, output2.getMetricUnit());

        final GetPlanningDistributionOutput output3 = output.get(2);
        assertEquals(A_DATE_UTC.toInstant(), output3.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output3.getDateOut().toInstant());
        assertEquals(1200, output3.getTotal());
        assertEquals(UNITS, output3.getMetricUnit());
    }

    @Test
    @DisplayName("Get planning distribution from forecast wih date in from and date in to")
    public void testGetPlanningDistributionWithDateInFromAndDateInToOk() {
        // GIVEN
        final ZonedDateTime dateInFrom = A_DATE_UTC.minusDays(3);
        final GetPlanningDistributionInput input = mockPlanningDistributionInput(
                dateInFrom, A_DATE_UTC);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateOutFrom())
                .dateTo(input.getDateOutTo())
                .build())
        ).thenReturn(mockForecastIds());

        when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
                A_DATE_UTC,
                A_DATE_UTC.plusDays(3),
                dateInFrom,
                A_DATE_UTC,
                mockForecastIds())
        ).thenReturn(planningDistributions());

        // WHEN
        final List<GetPlanningDistributionOutput> output = planningDistributionService
                .getPlanningDistribution(input);

        // THEN
        final GetPlanningDistributionOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
        assertEquals(1000, output1.getTotal());
        assertEquals(UNITS, output1.getMetricUnit());

        final GetPlanningDistributionOutput output3 = output.get(2);
        assertEquals(A_DATE_UTC.toInstant(), output3.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output3.getDateOut().toInstant());
        assertEquals(1200, output3.getTotal());
        assertEquals(UNITS, output3.getMetricUnit());
    }


    @Test
    @DisplayName("Get planning distribution applying current planning distribution")
    public void testGetPlanningDistributionApplyingCurrentPlanningDistribution() {
        // GIVEN
        final GetPlanningDistributionInput input = mockPlanningDistributionInput(null, null);

        final List<CurrentPlanningDistribution> distributions = currentPlanningDistributions();

        when(currentPlanningDistRepository
                .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                        FBM_WMS_OUTBOUND,
                        WAREHOUSE_ID,
                        A_DATE_UTC,
                        A_DATE_UTC.plusDays(3)
                )).thenReturn(distributions);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateOutFrom())
                .dateTo(input.getDateOutTo())
                .build())
        ).thenReturn(mockForecastIds());

        when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
                A_DATE_UTC,
                A_DATE_UTC.plusDays(3),
                null,
                null,
                mockForecastIds())
        ).thenReturn(planningDistributions());

        // WHEN
        final List<GetPlanningDistributionOutput> output = planningDistributionService
                .getPlanningDistribution(input);

        // THEN
        final GetPlanningDistributionOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
        assertEquals(1000, output1.getTotal());
        assertFalse(output1.isDeferred());

        final List<GetPlanningDistributionOutput> recordsForSecondDay =
                output.stream()
                        .filter(item -> item.getDateOut()
                                .toInstant()
                                .equals(A_DATE_UTC.plusDays(2).toInstant()))
                        .collect(Collectors.toUnmodifiableList());

        final Long outputTotalForSecondDay = recordsForSecondDay.stream()
                .map(GetPlanningDistributionOutput::getTotal)
                .reduce(0L, Long::sum);

        assertEquals(2, recordsForSecondDay.size());
        assertEquals(Long.valueOf(3700), outputTotalForSecondDay);
        assertTrue(recordsForSecondDay.stream()
                .allMatch(GetPlanningDistributionOutput::isDeferred)
        );
    }

    @Test
    @DisplayName("Get planning distribution applying both: current planning distribution and forecast deviation")
    public void testGetPlanningDistributionApplyingCurrentPlanningDistributionAndForecastDeviation() {
        // GIVEN
        final GetPlanningDistributionInput input = GetPlanningDistributionInput.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(FBM_WMS_OUTBOUND)
                .dateOutFrom(A_DATE_UTC)
                .dateOutTo(A_DATE_UTC.plusDays(3))
                .applyDeviation(true)
                .build();


        final List<CurrentPlanningDistribution> distributions = currentPlanningDistributions();

        when(currentPlanningDistRepository
                .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                        FBM_WMS_OUTBOUND,
                        WAREHOUSE_ID,
                        A_DATE_UTC,
                        A_DATE_UTC.plusDays(3)
                )).thenReturn(distributions);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateOutFrom())
                .dateTo(input.getDateOutTo())
                .build())
        ).thenReturn(mockForecastIds());

        when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
                A_DATE_UTC,
                A_DATE_UTC.plusDays(3),
                null,
                null,
                mockForecastIds())
        ).thenReturn(planningDistributions());

        when(currentForecastDeviationRepository.findByLogisticCenterIdAndWorkflowAndIsActiveTrue(WAREHOUSE_ID, FBM_WMS_OUTBOUND))
                .thenReturn(
                        List.of(new CurrentForecastDeviation(
                                1, input.getWarehouseId(), A_DATE_UTC, A_DATE_UTC, 1.0, true, 123L,
                                FBM_WMS_OUTBOUND, A_DATE_UTC, A_DATE_UTC
                        ))
            );

        // WHEN
        final List<GetPlanningDistributionOutput> output = planningDistributionService
                .getPlanningDistribution(input);

        // THEN
        final GetPlanningDistributionOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
        assertEquals(2000, output1.getTotal());
        assertFalse(output1.isDeferred());

        final List<GetPlanningDistributionOutput> recordsForSecondDay =
                output.stream()
                        .filter(item -> item.getDateOut()
                                .toInstant()
                                .equals(A_DATE_UTC.plusDays(2).toInstant()))
                        .collect(Collectors.toUnmodifiableList());

        final Long outputTotalForSecondDay = recordsForSecondDay.stream()
                .map(GetPlanningDistributionOutput::getTotal)
                .reduce(0L, Long::sum);

        assertEquals(2, recordsForSecondDay.size());
        assertEquals(Long.valueOf(4900), outputTotalForSecondDay);
        assertTrue(recordsForSecondDay.stream()
                .allMatch(GetPlanningDistributionOutput::isDeferred)
        );
    }


    @Test
    @DisplayName("Get planning distribution duplicate key")
    public void testGetPlanningDistributionDuplicateKey() {

        try {
            // GIVEN
            final GetPlanningDistributionInput input = mockPlanningDistributionInput(null, null);

            final CurrentPlanningDistribution first = mock(CurrentPlanningDistribution.class);
            final CurrentPlanningDistribution second = mock(CurrentPlanningDistribution.class);

            final List<CurrentPlanningDistribution> currentPlanningDistributions =
                    List.of(first, second);

            when(first.getDateOut()).thenReturn(A_DATE_UTC);
            when(second.getDateOut()).thenReturn(A_DATE_UTC);

            when(currentPlanningDistRepository
                    .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                            FBM_WMS_OUTBOUND,
                            WAREHOUSE_ID,
                            A_DATE_UTC,
                            A_DATE_UTC.plusDays(3)
                    )).thenReturn(currentPlanningDistributions);

            // WHEN
            planningDistributionService.getPlanningDistribution(input);

        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Duplicate key"));
        }
    }
}
