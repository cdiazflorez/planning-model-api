package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.util.DateUtils.ignoreMinutes;
import static java.util.stream.Collectors.toMap;

@Service
@AllArgsConstructor
public class WavingBacklogProjectionUseCase implements BacklogProjectionUseCase {

    @Override
    public boolean supportsProcessName(final ProcessName processName) {
        return processName == WAVING;
    }

    @Override
    public ProcessParams execute(final BacklogProjectionInput input) {
        final Map<ZonedDateTime, Long> planningSalesByDate = input.getPlanningUnits().stream()
                .collect(toMap(o -> ignoreMinutes(o.getDateIn()),
                        GetPlanningDistributionOutput::getTotal,
                        Long::sum));

        final Map<ZonedDateTime, Integer> wavingCapacityByDate = input.getThroughputs().stream()
                .collect(toMap(
                        EntityOutput::getDate,
                        entityOutput -> (int) entityOutput.getValue(),
                        Math::min));

        final long currentBacklog = input.getCurrentBacklogs().stream()
                .filter(cb -> WAVING == cb.getProcessName())
                .findFirst()
                .orElseThrow(() -> new BadRequestException("No current backlog for Waving"))
                .getQuantity();

        return ProcessParams.builder()
                .processName(WAVING)
                .currentBacklog(currentBacklog)
                .planningUnitsByDate(planningSalesByDate)
                .capacityByDate(wavingCapacityByDate)
                .build();
    }
}
