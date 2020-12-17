package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.BacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.output.EntityOutput;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static java.util.stream.Collectors.toMap;

@Service
@AllArgsConstructor
public class PackingBacklogProjectionUseCase implements BacklogProjectionUseCase {

    @Override
    public boolean supportsProcessName(final ProcessName processName) {
        return processName == PACKING;
    }

    @Override
    public ProcessParams execute(final BacklogProjectionInput input) {
        final Map<ZonedDateTime, Long> previousProcessCapacity = input.getThroughputs().stream()
                .filter(e -> e.getProcessName() == PACKING.getPreviousProcesses().get(0))
                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue));

        final Map<ZonedDateTime, Integer> packingCapacity = input.getThroughputs().stream()
                .filter(e -> e.getProcessName() == PACKING)
                .collect(toMap(EntityOutput::getDate, e -> (int) e.getValue()));

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
