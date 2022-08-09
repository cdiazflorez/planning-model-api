package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
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
public class PackingRegularBacklogProjectionUseCase implements GetBacklogProjectionParamsUseCase {

  @Override
  public boolean supportsProcessName(final ProcessName processName) {
    return processName == PACKING;
  }

  @Override
  public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {
    final Map<ZonedDateTime, Long> previousProcessCapacity = input.getThroughputs().stream()
        .filter(e -> PACKING.getPreviousProcesses() == e.getProcessName())
        .collect(toMap(EntityOutput::getDate, item -> getPackingRegularDistribution(item, input.getPackingWallRatios())));

    final Map<ZonedDateTime, Long> packingRegularCapacityByDate = input.getThroughputs().stream()
        .filter(e -> e.getProcessName() == PACKING)
        .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, Long::sum));

    final long currentBacklog = input.getCurrentBacklogs().stream()
        .filter(cb -> PACKING == cb.getProcessName())
        .findFirst().map(CurrentBacklog::getQuantity).orElse(0);

    return ProcessParams.builder()
        .processName(PACKING)
        .currentBacklog(currentBacklog)
        .planningUnitsByDate(previousProcessCapacity)
        .capacityByDate(packingRegularCapacityByDate)
        .build();
  }

  private long getPackingRegularDistribution(final EntityOutput item, final Map<ZonedDateTime, Double> packingWallRatios) {
    final Double packingWallRatio = packingWallRatios.getOrDefault(item.getDate(), 0.00);
    return (long) (item.getValue() * (1 - packingWallRatio));
  }
}
