package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.ignoreMinutes;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class WavingBacklogProjectionUseCase implements GetBacklogProjectionParamsUseCase {

  private final GetCapacityPerHourService getCapacityUseCase;

  @Override
  public boolean supportsProcessName(final ProcessName processName) {
    return processName == WAVING;
  }

  @Override
  public ProcessParams execute(final ProcessName processName, final BacklogProjectionInput input) {

    final Map<ZonedDateTime, Long> planningSalesByDate = input.getPlanningUnits().stream()
        .collect(
            toMap(
                distributionOutput -> ignoreMinutes(ofInstant(distributionOutput.getDateIn(), UTC)),
                distributionOutput -> Math.round(distributionOutput.getTotal()),
                Long::sum)
        );

    final Map<ZonedDateTime, Long> wavingCapacityByDate = getCapacityUseCase.execute(
            FBM_WMS_OUTBOUND,
            CapacityInput.fromEntityOutputs(input.getThroughputs()))
        .stream()
        .collect(toMap(CapacityOutput::getDate, CapacityOutput::getValue, (v1, v2) -> v2));

    final long currentBacklog = input.getCurrentBacklogs().stream()
        .filter(cb -> WAVING == cb.getProcessName())
        .findFirst().map(CurrentBacklog::getQuantity).orElse(0);

    return ProcessParams.builder()
        .processName(WAVING)
        .currentBacklog(currentBacklog)
        .planningUnitsByDate(planningSalesByDate)
        .capacityByDate(wavingCapacityByDate)
        .build();
  }
}
