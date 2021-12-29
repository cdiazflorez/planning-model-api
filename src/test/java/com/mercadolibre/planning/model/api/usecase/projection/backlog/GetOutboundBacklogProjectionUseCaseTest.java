package com.mercadolibre.planning.model.api.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.GetOutboundBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjection;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjectionOutputValue;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetOutboundBacklogProjectionUseCaseTest {

    @InjectMocks
    private GetOutboundBacklogProjectionUseCase useCase;

    @Mock
    private CalculateBacklogProjectionUseCase calculateBacklogProjection;

    @Mock
    private GetThroughputUseCase getThroughputUseCase;

    @Mock
    private PlanningDistributionService planningDistributionService;

    @Test
    public void testExecute() {
        //GIVEN
        final ZonedDateTime now = ZonedDateTime.now();

        final BacklogProjectionInput input = BacklogProjectionInput.builder()
                .logisticCenterId("ARBA01")
                .dateFrom(now)
                .dateTo(now.plusHours(8))
                .processNames(List.of(ProcessName.WAVING, ProcessName.PICKING))
                .build();
        final List<BacklogProjection> backlogProjection = mockBacklogProjection();

        when(getThroughputUseCase.execute(GetEntityInput.builder()
                .warehouseId(input.getLogisticCenterId())
                .dateFrom(input.getDateFrom().minusHours(1))
                .dateTo(input.getDateTo())
                .processName(input.getProcessNames())
                .source(Source.SIMULATION)
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .build())).thenReturn(emptyList());

        when(planningDistributionService.getPlanningDistribution(GetPlanningDistributionInput.builder()
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .warehouseId(input.getLogisticCenterId())
                .dateInTo(input.getDateTo().plusDays(1))
                .dateOutFrom(input.getDateFrom())
                .dateOutTo(input.getDateFrom().plusDays(1))
                .applyDeviation(true)
                .build())).thenReturn(emptyList());

        when(calculateBacklogProjection.execute(BacklogProjectionInput.builder()
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .throughputs(emptyList())
                .currentBacklogs(input.getCurrentBacklogs())
                .processNames(input.getProcessNames())
                .planningUnits(Collections.emptyList())
                .build())).thenReturn(backlogProjection);

        //WHEN
        final List<BacklogProjection> response = useCase.execute(input);

        //THEN
        assertEquals(backlogProjection, response);
    }

    private List<BacklogProjection> mockBacklogProjection() {
        return List.of(new BacklogProjection(
                ProcessName.PICKING,
                List.of(BacklogProjectionOutputValue.builder()
                        .date(ZonedDateTime.now())
                        .quantity(200L)
                        .build()),
                Source.FORECAST
        ));
    }
}
