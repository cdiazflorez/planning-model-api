package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_REGULAR;
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
public class PackingRegularBacklogProjectionUseCase implements GetBacklogProjectionParamsUseCase {

    private static final Map<ProcessName, ProcessName> PROCESS_BY_PROCESS = Map.of(PACKING_REGULAR, PACKING);

    @Override
    public boolean supportsProcessName(final ProcessName processName) {
        return processName == PACKING_REGULAR;
    }

    @Override
    public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {
        final Map<ZonedDateTime, Long> previousProcessCapacity = input.getThroughputs().stream()
                .filter(e -> PACKING_REGULAR.getPreviousProcesses() == e.getProcessName())
                .collect(toMap(EntityOutput::getDate, item -> (long) (item.getValue() * input.getRatioPackingRegular()), (v1, v2) -> v2));

        final Map<ZonedDateTime, Long> packingCapacity = input.getThroughputs().stream()
                .filter(e -> e.getProcessName() == PROCESS_BY_PROCESS.get(PACKING_REGULAR))
                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, Long::sum));

        final long currentBacklog = input.getCurrentBacklogs().stream()
                .filter(cb -> PACKING_REGULAR == cb.getProcessName())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No current backlog for Packing"))
                .getQuantity();

        return ProcessParams.builder()
                .processName(PACKING_REGULAR)
                .currentBacklog(currentBacklog)
                .planningUnitsByDate(previousProcessCapacity)
                .capacityByDate(packingCapacity)
                .build();
    }
}
