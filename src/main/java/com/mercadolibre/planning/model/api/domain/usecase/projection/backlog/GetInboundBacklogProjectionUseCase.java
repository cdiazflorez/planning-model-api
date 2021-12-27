package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjection;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.RECEIVING;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;

@Component
@AllArgsConstructor
public class GetInboundBacklogProjectionUseCase implements GetBacklogProjectionUseCase {

    private final CalculateBacklogProjectionUseCase calculateBacklogProjection;

    private final GetThroughputUseCase getThroughputUseCase;

    @Override
    public List<BacklogProjection> execute(final BacklogProjectionInput input) {
        // Esto es temporal hasta que agreguen la columna de Reps Sistemicos de Receiving al forecast
        final List<ProcessName> tphProcessNames = new ArrayList<>(input.getProcessNames());
        tphProcessNames.add(RECEIVING);

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
                .builder()
                .warehouseId(input.getLogisticCenterId())
                .dateFrom(input.getDateFrom().minusHours(1))
                .dateTo(input.getDateTo())
                .source(SIMULATION)
                .processName(tphProcessNames)
                .workflow(getWorkflow())
                .build());

        return calculateBacklogProjection.execute(BacklogProjectionInput.builder()
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .throughputs(throughput)
                .currentBacklogs(input.getCurrentBacklogs())
                .processNames(input.getProcessNames())
                .planningUnits(Collections.emptyList())
                .build());
    }

    @Override
    public Workflow getWorkflow() {
        return Workflow.FBM_WMS_INBOUND;
    }
}
