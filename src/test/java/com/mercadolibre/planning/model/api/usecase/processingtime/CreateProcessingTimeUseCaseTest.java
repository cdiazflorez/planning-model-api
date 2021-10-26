package com.mercadolibre.planning.model.api.usecase.processingtime;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingTimeRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeOutput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeUseCase;
import com.mercadolibre.planning.model.api.usecase.PlanningDistributionViewImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static java.time.ZonedDateTime.parse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.ExcessiveImports")
@ExtendWith(MockitoExtension.class)
public class CreateProcessingTimeUseCaseTest {

    @InjectMocks
    private CreateProcessingTimeUseCase createProcessingTimeUseCase;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @Mock
    private CurrentProcessingTimeRepository processingTimeRepository;

    @Mock
    private PlanningDistributionRepository planningDistributionRepository;

    @Mock
    private CurrentPlanningDistributionRepository currentPlanningDistributionRep;

    private final ZonedDateTime cptFrom = parse("2020-01-07T00:00:00Z");
    private final ZonedDateTime cptTo = parse("2020-01-08T15:00:00Z");
    private static final String WAREHOUSE_ID = "ARBA01";
    private static final ZonedDateTime GENERIC_DATE = parse("2020-01-07T08:00:00Z[UTC]");

    @ParameterizedTest
    @MethodSource("getDataMock")
    public void testCreateProcessingAndCurrentPlanningDistributions(
            final List<CurrentPlanningDistribution> currentPlanningDistributions,
            final List<PlanningDistributionView> planningDistributions,
            final List<CurrentPlanningDistribution> expectedDistributions) {

        // GIVEN
        final CreateProcessingTimeInput input =
                CreateProcessingTimeInput.builder()
                        .value(360)
                        .metricUnit(MINUTES)
                        .logisticCenterId(WAREHOUSE_ID)
                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                        .cptFrom(cptFrom)
                        .cptTo(cptTo)
                        .userId(1234)
                        .build();

        final CurrentProcessingTime currentProcessingTimeEntity =
                CurrentProcessingTime.builder()
                        .value(input.getValue())
                        .logisticCenterId(input.getLogisticCenterId())
                        .metricUnit(input.getMetricUnit())
                        .workflow(input.getWorkflow())
                        .cptFrom(input.getCptFrom())
                        .cptTo(input.getCptTo())
                        .userId(input.getUserId())
                        .build();

        // WHEN
        when(currentPlanningDistributionRep
                .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                        input.getWorkflow(),
                        input.getLogisticCenterId(),
                        input.getCptFrom(),
                        input.getCptTo())).thenReturn(currentPlanningDistributions);

        when(planningDistributionRepository.findByWarehouseIdWorkflowAndCptRange(
                input.getCptFrom(),
                input.getCptTo(),
                getForecastIds(),
                true))
                .thenReturn(planningDistributions);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getLogisticCenterId())
                .dateFrom(input.getCptFrom())
                .dateTo(input.getCptTo())
                .build())).thenReturn(getForecastIds());

        when(processingTimeRepository.save(currentProcessingTimeEntity))
                .thenReturn(currentProcessingTimeEntity);

        final CreateProcessingTimeOutput response =
                createProcessingTimeUseCase.execute(input);

        assertEquals(response.getValue(), input.getValue());
        assertEquals(response.getMetricUnit(), input.getMetricUnit());
        assertEquals(response.getLogisticCenterId(), input.getLogisticCenterId());

        verify(currentPlanningDistributionRep).saveAll(expectedDistributions);
    }

    public static Stream<Arguments> getDataMock() {

        // (+): IsActive = true
        // (-): IsActive = false
        // (*): Interseccion
        return Stream.of(
                // Interseccion Parcial
                // CurrentDistribution -> PlanningDistribution = expectedDistributions
                // 1, 2, 3 -> _, 2*, _ = +2, -2
                Arguments.of(
                        List.of(
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(1))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(2))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(3))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build()),
                        List.of(
                                new PlanningDistributionViewImpl(
                                        1,
                                        Date.from(GENERIC_DATE.toInstant()),
                                        Date.from(GENERIC_DATE.plusHours(2).toInstant()),
                                        900,
                                        UNITS)),
                        List.of(
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE.minusHours(4))
                                        .dateOut(GENERIC_DATE.plusHours(2))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(2))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(false)
                                        .build())
                ),
                // Agregacion Total
                // CurrentDistribution -> PlanningDistribution = expectedDistributions
                // 1, 2, 3 -> 4, 5, 6 = +4, +5, +6
                Arguments.of(
                        List.of(
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(1))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(2))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(3))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build()),
                        List.of(
                                new PlanningDistributionViewImpl(
                                        1,
                                        Date.from(GENERIC_DATE.toInstant()),
                                        Date.from(GENERIC_DATE.plusHours(4).toInstant()),
                                        700,
                                        UNITS),
                                new PlanningDistributionViewImpl(
                                        1,
                                        Date.from(GENERIC_DATE.toInstant()),
                                        Date.from(GENERIC_DATE.plusHours(5).toInstant()),
                                        900,
                                        UNITS),
                                new PlanningDistributionViewImpl(
                                        1,
                                        Date.from(GENERIC_DATE.toInstant()),
                                        Date.from(GENERIC_DATE.plusHours(6).toInstant()),
                                        1200,
                                        UNITS)),
                        List.of(
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE.minusHours(2))
                                        .dateOut(GENERIC_DATE.plusHours(4))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE.minusHours(1))
                                        .dateOut(GENERIC_DATE.plusHours(5))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(6))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build())

                ),
                // Interseccion Total
                // CurrentDistribution -> PlanningDistribution = expectedDistributions
                // 1, 2, 3 -> 1, 2, 3 = +1, -1, +2, -2, +3, -3
                Arguments.of(
                        List.of(
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(1))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(2))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(3))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build()),
                        List.of(
                                new PlanningDistributionViewImpl(
                                        1,
                                        Date.from(GENERIC_DATE.toInstant()),
                                        Date.from(GENERIC_DATE.plusHours(1).toInstant()),
                                        700,
                                        UNITS),
                                new PlanningDistributionViewImpl(
                                        1,
                                        Date.from(GENERIC_DATE.toInstant()),
                                        Date.from(GENERIC_DATE.plusHours(2).toInstant()),
                                        900,
                                        UNITS),
                                new PlanningDistributionViewImpl(
                                        1,
                                        Date.from(GENERIC_DATE.toInstant()),
                                        Date.from(GENERIC_DATE.plusHours(3).toInstant()),
                                        1200,
                                        UNITS)),
                        List.of(
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE.minusHours(5))
                                        .dateOut(GENERIC_DATE.plusHours(1))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(1))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(false)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE.minusHours(4))
                                        .dateOut(GENERIC_DATE.plusHours(2))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(2))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(false)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE.minusHours(3))
                                        .dateOut(GENERIC_DATE.plusHours(3))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(true)
                                        .build(),
                                CurrentPlanningDistribution.builder()
                                        .logisticCenterId(WAREHOUSE_ID)
                                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                                        .dateInFrom(GENERIC_DATE)
                                        .dateOut(GENERIC_DATE.plusHours(3))
                                        .quantity(0)
                                        .quantityMetricUnit(UNITS)
                                        .isActive(false)
                                        .build())
                )
        );
    }

    private List<Long> getForecastIds() {
        return List.of(1L, 2L, 3L);
    }
}
