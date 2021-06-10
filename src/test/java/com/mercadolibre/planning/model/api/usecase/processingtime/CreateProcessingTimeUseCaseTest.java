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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private final ZonedDateTime cptFrom = parse("2020-01-01T15:00:00Z");
    private final ZonedDateTime cptTo = parse("2020-01-12T15:00:00Z");
    private final ZonedDateTime dateOut = parse("2020-01-07T15:00:00Z[UTC]");
    private final ZonedDateTime dateInFrom = parse("2020-01-07T09:00:00Z[UTC]");

    @Test
    public void testCreateProcessingTime() {

        final List<PlanningDistributionView> planningDistributionViews = planningDistributions();

        final CurrentPlanningDistribution first = mock(CurrentPlanningDistribution.class);
        final CurrentPlanningDistribution second = mock(CurrentPlanningDistribution.class);

        final List<CurrentPlanningDistribution> currentPlanningDistributions =
                List.of(first, second);

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

        final List<CurrentPlanningDistribution> inputCurrentPlanningDist =
                getCurrentPlanningDistributionList();

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
                .thenReturn(planningDistributionViews);

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

        verify(currentPlanningDistributionRep).saveAll(inputCurrentPlanningDist);
    }

    private List<CurrentPlanningDistribution> getCurrentPlanningDistributionList() {

        return List.of(
                CurrentPlanningDistribution.builder()
                        .logisticCenterId("ARBA01")
                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                        .dateInFrom(dateInFrom.plusDays(1))
                        .dateOut(dateOut.plusDays(1))
                        .quantity(0)
                        .quantityMetricUnit(UNITS)
                        .isActive(true)
                        .build(),
                CurrentPlanningDistribution.builder()
                        .logisticCenterId("ARBA01")
                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                        .dateInFrom(dateInFrom.plusDays(2))
                        .dateOut(dateOut.plusDays(2))
                        .quantity(0)
                        .quantityMetricUnit(UNITS)
                        .isActive(true)
                        .build(),
                CurrentPlanningDistribution.builder()
                        .logisticCenterId("ARBA01")
                        .workflow(Workflow.FBM_WMS_OUTBOUND)
                        .dateInFrom(dateInFrom.plusDays(3))
                        .dateOut(dateOut.plusDays(3))
                        .quantity(0)
                        .quantityMetricUnit(UNITS)
                        .isActive(true)
                        .build());
    }

    public List<PlanningDistributionView> planningDistributions() {
        return List.of(
                new PlanningDistributionViewImpl(
                        Date.from(dateOut.toInstant()),
                        Date.from(dateOut.plusDays(1).toInstant()),
                        1000,
                        UNITS),
                new PlanningDistributionViewImpl(
                        Date.from(dateOut.toInstant()),
                        Date.from(dateOut.plusDays(2).toInstant()),
                        1200,
                        UNITS),
                new PlanningDistributionViewImpl(
                        Date.from(dateOut.toInstant()),
                        Date.from(dateOut.plusDays(3).toInstant()),
                        1250,
                        UNITS)
        );
    }

    private List<Long> getForecastIds() {
        return List.of(1L, 2L, 3L);
    }
}
