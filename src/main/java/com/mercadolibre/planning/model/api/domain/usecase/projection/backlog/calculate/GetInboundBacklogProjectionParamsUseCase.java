package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Service
@AllArgsConstructor
public class GetInboundBacklogProjectionParamsUseCase implements GetBacklogProjectionParamsUseCase {

    @Override
    public boolean supportsProcessName(final ProcessName processName) {
        return processName == ProcessName.CHECK_IN || processName == ProcessName.PUT_AWAY;
    }

    @Override
    public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {

        final long currentBacklog = input.getCurrentBacklogs().stream()
                .filter(cb -> processName == cb.getProcessName())
                .findFirst()
                .orElseGet(() -> new CurrentBacklog(processName, 0))
                .getQuantity();

        return ProcessParams.builder()
                .processName(processName)
                .currentBacklog(currentBacklog)
                .planningUnitsByDate(
                        filterCapacityByProcess(input.getThroughputs(), processName.getPreviousProcesses())
                )
                .capacityByDate(filterCapacityByProcess(input.getThroughputs(), processName))
                .build();
    }

    private Map<ZonedDateTime, Long> filterCapacityByProcess(final List<EntityOutput> throughput,
                                                             final ProcessName processName) {

        return throughput.stream()
                .filter(e -> e.getProcessName() == processName)
                .collect(toMap(EntityOutput::getDate, EntityOutput::getRoundedValue, Long::sum));
    }
}
