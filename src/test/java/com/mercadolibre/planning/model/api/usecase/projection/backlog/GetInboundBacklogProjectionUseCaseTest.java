package com.mercadolibre.planning.model.api.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.GetInboundBacklogProjectionUseCase;
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
import java.util.ArrayList;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.RECEIVING;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetInboundBacklogProjectionUseCaseTest {

    @InjectMocks
    private GetInboundBacklogProjectionUseCase useCase;

    @Mock
    private CalculateBacklogProjectionUseCase calculateBacklogProjection;

    @Mock
    private GetThroughputUseCase getThroughputUseCase;

    @Test
    public void testExecute() {
        //GIVEN
        final ZonedDateTime now = ZonedDateTime.now();

        final BacklogProjectionInput input = BacklogProjectionInput.builder()
                .logisticCenterId("ARBA01")
                .dateFrom(now)
                .dateTo(now.plusHours(8))
                .processNames(List.of(ProcessName.CHECK_IN, ProcessName.PUT_AWAY))
                .build();
        final List<BacklogProjection> backlogProjection = mockBacklogProjection();
        final List<ProcessName> tphProcessNames = new ArrayList<>(input.getProcessNames());
        tphProcessNames.add(RECEIVING);

        when(getThroughputUseCase.execute(GetEntityInput.builder()
                        .warehouseId(input.getLogisticCenterId())
                        .dateFrom(input.getDateFrom().minusHours(1))
                        .dateTo(input.getDateTo())
                        .processName(tphProcessNames)
                        .source(Source.SIMULATION)
                        .workflow(Workflow.FBM_WMS_INBOUND)
                .build())).thenReturn(emptyList());

        when(calculateBacklogProjection.execute(BacklogProjectionInput.builder()
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .throughputs(emptyList())
                .currentBacklogs(input.getCurrentBacklogs())
                .processNames(input.getProcessNames())
                .planningUnits(emptyList())
                .build())).thenReturn(backlogProjection);

        //WHEN
        final List<BacklogProjection> response = useCase.execute(input);

        //THEN
        assertEquals(backlogProjection, response);
    }

    private List<BacklogProjection> mockBacklogProjection() {
        return List.of(new BacklogProjection(
                ProcessName.CHECK_IN,
                List.of(BacklogProjectionOutputValue.builder()
                        .date(ZonedDateTime.now())
                        .quantity(1L)
                        .build()),
                Source.FORECAST
        ));
    }
}
