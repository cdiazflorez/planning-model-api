package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PackingWallBacklogProjectionUseCase implements GetBacklogProjectionParamsUseCase {

  @Override
  public boolean supportsProcessName(final ProcessName processName) {
    return processName == PACKING_WALL;
  }

  @Override
  public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {
    final Map<ZonedDateTime, Long> previousProcessCapacity = input.getThroughputs().stream()
        .filter(e -> e.getProcessName() == PACKING_WALL.getPreviousProcesses())
        .collect(toMap(EntityOutput::getDate, EntityOutput::getLongValue, (v1, v2) -> v2));

    final Map<ZonedDateTime, Long> packingWallCapacityByDate = input.getThroughputs().stream()
        .filter(e -> e.getProcessName() == PACKING_WALL)
        .collect(toMap(EntityOutput::getDate, EntityOutput::getLongValue, Long::sum));

    final long currentBacklog = input.getCurrentBacklogs().stream()
        .filter(cb -> cb.getProcessName() == PACKING_WALL)
        .findFirst().map(CurrentBacklog::getQuantity).orElse(0);

    return ProcessParams.builder()
        .processName(PACKING_WALL)
        .currentBacklog(currentBacklog)
        .planningUnitsByDate(previousProcessCapacity)
        .capacityByDate(packingWallCapacityByDate)
        .build();
  }
}
