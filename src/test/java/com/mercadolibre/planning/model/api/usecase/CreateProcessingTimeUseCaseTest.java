package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingTimeRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeOutput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.planningDistributions;
import static java.time.ZonedDateTime.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    public void testCreateProcessingTime() {

        final List<PlanningDistributionView> planningDistributionData = planningDistributions();
        final ZonedDateTime cptFrom = parse("2020-01-01T11:00:00Z[UTC]");
        final ZonedDateTime cptTo = parse("2020-01-02T10:00:00Z[UTC]");

        // GIVEN
        final CreateProcessingTimeInput input =
                CreateProcessingTimeInput.builder()
                        .value(360)
                        .metricUnit(MetricUnit.MINUTES)
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
                        .isActive(true)
                        .userId(input.getUserId())
                        .build();

        final List<CurrentPlanningDistribution> inputCurrentPlanningDist =
                getCurrentPlanningDistributionList(input, planningDistributionData);

        // WHEN
        when(planningDistributionRepository.findByWarehouseIdWorkflowAndCptRange(
                input.getCptFrom(),
                input.getCptTo(),
                getForecastIds(),
                true))
                .thenReturn(planningDistributionData);

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

    private List<CurrentPlanningDistribution> getCurrentPlanningDistributionList(
            final CreateProcessingTimeInput input,
            final List<PlanningDistributionView> planningDistributionViews) {

        return planningDistributionViews.stream().map(pd -> CurrentPlanningDistribution
                .builder()
                .workflow(input.getWorkflow())
                .logisticCenterId(input.getLogisticCenterId())
                .dateOut(pd.getDateOut().toInstant()
                        .atZone(ZoneId.systemDefault()))
                .quantity(0)
                .quantityMetricUnit(MetricUnit.UNITS)
                .isActive(true)
                .build())
                .collect(Collectors.toList());
    }

    private List<Long> getForecastIds() {
        return List.of(1L, 2L, 3L);
    }
}
