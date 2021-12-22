package com.mercadolibre.planning.model.api.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.GetInboundBacklogProjectionParamsUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.ProcessParams;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetInboundBacklogProjectionParamsUseCastTest {

    private static final ZonedDateTime NOW = ZonedDateTime.now();

    private final GetInboundBacklogProjectionParamsUseCase useCase = new GetInboundBacklogProjectionParamsUseCase();

    @Test
    public void testExecute() {
        //GIVEN
        BacklogProjectionInput input = BacklogProjectionInput.builder()
                .throughputs(mockThroughput())
                .currentBacklogs(mockBacklogs())
                .build();

        //WHEN
        ProcessParams params = useCase.execute(CHECK_IN, input);

        //THEN
        assertEquals(CHECK_IN, params.getProcessName());
        assertEquals(20L, params.getCurrentBacklog());
        assertEquals(100, params.getPlanningUnitsByDate().get(NOW));
        assertEquals(150, params.getPlanningUnitsByDate().get(NOW.plusHours(1)));
        assertEquals(400, params.getCapacityByDate().get(NOW));
        assertEquals(70, params.getCapacityByDate().get(NOW.plusHours(1)));
    }

    private List<EntityOutput> mockThroughput() {
        return List.of(
                EntityOutput.builder().processName(STAGE_IN).date(NOW).value(100).build(),
                EntityOutput.builder().processName(STAGE_IN).date(NOW.plusHours(1)).value(150).build(),
                EntityOutput.builder().processName(CHECK_IN).date(NOW).value(200).build(),
                EntityOutput.builder().processName(CHECK_IN).date(NOW).value(200).build(),
                EntityOutput.builder().processName(CHECK_IN).date(NOW.plusHours(1)).value(70).build(),
                EntityOutput.builder().processName(PUT_AWAY).date(NOW).value(230).build(),
                EntityOutput.builder().processName(PUT_AWAY).date(NOW.plusHours(1)).value(240).build());
    }

    private List<CurrentBacklog> mockBacklogs() {
        return List.of(
                new CurrentBacklog(RECEIVING, 10),
                new CurrentBacklog(CHECK_IN, 20));
    }
}
