package com.mercadolibre.planning.model.api.domain.service.sla;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OutboundSlaPropertiesService {

  private final GetCycleTimeService getCycleTimeService;

  public Map<Instant, SlaProperties> get(final Input input) {
    final List<ZonedDateTime> slas = input.defaultSlas.stream().map(DateUtils::fromInstant).toList();

    final Map<ZonedDateTime, Long> cycleTimeBySla = getCycleTimeService.execute(new GetCycleTimeInput(input.logisticCenterId(), slas));

    return cycleTimeBySla.entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().toInstant(),
            entry -> new SlaProperties(entry.getValue()))
        );
  }

    public record Input(String logisticCenterId, Workflow workflow, List<Instant> defaultSlas) {
  }

}
