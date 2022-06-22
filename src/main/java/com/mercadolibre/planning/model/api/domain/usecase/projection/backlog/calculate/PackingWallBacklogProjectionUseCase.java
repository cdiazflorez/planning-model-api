package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL_PROCESS;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PackingWallBacklogProjectionUseCase implements GetBacklogProjectionParamsUseCase {

    private static final Map<ProcessName, ProcessName> PROCESS_BY_PROCESS = Map.of(PACKING_WALL_PROCESS, PACKING_WALL);

    @Override
    public boolean supportsProcessName(final ProcessName processName) {
        return processName == PACKING_WALL_PROCESS;
    }

    @Override
    public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {
        final Map<ZonedDateTime, Long> previousProcessCapacity = input.getThroughputs().stream()
                .filter(e -> e.getProcessName() == PACKING_WALL_PROCESS.getPreviousProcesses())
                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, (v1, v2) -> v2));

        final Map<ZonedDateTime, Long> packingCapacity = input.getThroughputs().stream()
                .filter(e -> e.getProcessName() == PROCESS_BY_PROCESS.get(PACKING_WALL_PROCESS))
                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, Long::sum));

        final long currentBacklog = input.getCurrentBacklogs().stream()
                .filter(cb -> cb.getProcessName() == PACKING_WALL_PROCESS)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No current backlog for Packing-Wall"))
                .getQuantity();

        return ProcessParams.builder()
                .processName(PACKING_WALL_PROCESS)
                .currentBacklog(currentBacklog)
                .planningUnitsByDate(previousProcessCapacity)
                .capacityByDate(packingCapacity)
                .build();
    }
}
