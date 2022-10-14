package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
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
public class WallInBacklogProjectionUseCase implements GetBacklogProjectionParamsUseCase {

  @Override
  public boolean supportsProcessName(final ProcessName processName) {
    return processName == WALL_IN;
  }

  @Override
  public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {
    final Map<ZonedDateTime, Long> previousProcessCapacity = input.getThroughputs().stream()
        .filter(e -> e.getProcessName() == WALL_IN.getPreviousProcesses())
        .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, (v1, v2) -> v2));

    final Map<ZonedDateTime, Long> wallInCapacityByDate = input.getThroughputs().stream()
        .filter(e -> e.getProcessName() == WALL_IN)
        .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, Long::sum));

    final long currentBacklog = input.getCurrentBacklogs().stream()
        .filter(cb -> cb.getProcessName() == WALL_IN)
        .findFirst().map(CurrentBacklog::getQuantity).orElse(0);

    return ProcessParams.builder()
        .processName(WALL_IN)
        .currentBacklog(currentBacklog)
        .planningUnitsByDate(previousProcessCapacity)
        .capacityByDate(wallInCapacityByDate)
        .build();
  }
}
