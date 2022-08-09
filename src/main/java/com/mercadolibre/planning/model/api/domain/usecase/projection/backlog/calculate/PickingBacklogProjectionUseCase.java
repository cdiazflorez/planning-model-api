package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PickingBacklogProjectionUseCase implements GetBacklogProjectionParamsUseCase {

  private final GetCapacityPerHourService getCapacityUseCase;

  @Override
  public boolean supportsProcessName(final ProcessName processName) {
    return processName == PICKING;
  }

  @Override
  public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {
    final List<CapacityOutput> previousProcessCapacity = getCapacityUseCase.execute(
        FBM_WMS_OUTBOUND,
        CapacityInput.fromEntityOutputs(input.getThroughputs()));

    final Map<ZonedDateTime, Long> pickingCapacityByDate = input.getThroughputs().stream()
        .filter(e -> e.getProcessName() == PICKING)
        .collect(toMap(EntityOutput::getDate, EntityOutput::getValue, (v1, v2) -> v2));

    final long currentBacklog = input.getCurrentBacklogs().stream()
        .filter(cb -> PICKING == cb.getProcessName())
        .findFirst().map(CurrentBacklog::getQuantity).orElse(0);

    return ProcessParams.builder()
        .processName(PICKING)
        .currentBacklog(currentBacklog)
        .planningUnitsByDate(previousProcessCapacity.stream().collect(
            toMap(CapacityOutput::getDate, CapacityOutput::getValue)))
        .capacityByDate(pickingCapacityByDate)
        .build();
  }
}
