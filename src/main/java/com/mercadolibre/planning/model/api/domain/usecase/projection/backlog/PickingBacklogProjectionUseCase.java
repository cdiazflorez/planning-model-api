package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.exception.BadRequestException;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static java.util.stream.Collectors.toMap;

@Service
@AllArgsConstructor
public class PickingBacklogProjectionUseCase implements BacklogProjectionUseCase {

    private final GetCapacityPerHourUseCase getCapacityUseCase;

    @Override
    public boolean supportsProcessName(final ProcessName processName) {
        return processName == PICKING;
    }

    @Override
    public ProcessParams execute(final BacklogProjectionInput input) {
        final List<CapacityOutput> previousProcessCapacity = getCapacityUseCase.execute(
                CapacityInput.fromEntityOutputs(input.getThroughputs()));

        final Map<ZonedDateTime, Integer> pickingCapacity = input.getThroughputs().stream()
                .filter(e -> e.getProcessName() == PICKING)
                .collect(toMap(EntityOutput::getDate, e -> (int) e.getValue()));

        final long currentBacklog = input.getCurrentBacklogs().stream()
                .filter(cb -> PICKING == cb.getProcessName())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No current backlog for Picking"))
                .getQuantity();

        return ProcessParams.builder()
                .processName(PICKING)
                .currentBacklog(currentBacklog)
                .planningUnitsByDate(previousProcessCapacity.stream().collect(
                        toMap(CapacityOutput::getDate, CapacityOutput::getValue)))
                .capacityByDate(pickingCapacity)
                .build();
    }
}
