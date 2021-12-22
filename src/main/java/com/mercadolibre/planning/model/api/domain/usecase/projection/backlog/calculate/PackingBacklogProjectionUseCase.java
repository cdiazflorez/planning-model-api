package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static java.util.stream.Collectors.toMap;

@Service
@AllArgsConstructor
public class PackingBacklogProjectionUseCase implements GetBacklogProjectionParamsUseCase {

    @Override
    public boolean supportsProcessName(final ProcessName processName) {
        return processName == PACKING;
    }

    @Override
    public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {
        final Map<ZonedDateTime, Long> previousProcessCapacity = input.getThroughputs().stream()
                .filter(e -> e.getProcessName() == PACKING.getPreviousProcesses())
                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, (v1, v2) -> v2));

        final Map<ZonedDateTime, Long> packingCapacity = input.getThroughputs().stream()
                .filter(e -> List.of(PACKING, PACKING_WALL).contains(e.getProcessName()))
                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, Long::sum));

        final long currentBacklog = input.getCurrentBacklogs().stream()
                .filter(cb -> PACKING == cb.getProcessName())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No current backlog for Packing"))
                .getQuantity();

        return ProcessParams.builder()
                .processName(PACKING)
                .currentBacklog(currentBacklog)
                .planningUnitsByDate(previousProcessCapacity)
                .capacityByDate(packingCapacity)
                .build();
    }
}
