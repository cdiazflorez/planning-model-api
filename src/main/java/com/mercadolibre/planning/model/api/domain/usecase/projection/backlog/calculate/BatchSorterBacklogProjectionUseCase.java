package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
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
public class BatchSorterBacklogProjectionUseCase implements GetBacklogProjectionParamsUseCase {

    @Override
    public boolean supportsProcessName(final ProcessName processName) {
        return processName == BATCH_SORTER;
    }

    @Override
    public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {
        final Map<ZonedDateTime, Long> previousProcessCapacity = input.getThroughputs().stream()
                .filter(e -> e.getProcessName() == BATCH_SORTER.getPreviousProcesses())
                .collect(toMap(EntityOutput::getDate, item -> (long) (item.getValue() * (1 - input.getRatioPackingRegular())),
                        (v1, v2) -> v2));

        final Map<ZonedDateTime, Long> packingCapacity = input.getThroughputs().stream()
                .filter(e -> e.getProcessName() == BATCH_SORTER)
                .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, Long::sum));

        final long currentBacklog = input.getCurrentBacklogs().stream()
                .filter(cb -> BATCH_SORTER == cb.getProcessName())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No current backlog for Consolidation"))
                .getQuantity();

        return ProcessParams.builder()
                .processName(BATCH_SORTER)
                .currentBacklog(currentBacklog)
                .planningUnitsByDate(previousProcessCapacity)
                .capacityByDate(packingCapacity)
                .build();
    }
}
