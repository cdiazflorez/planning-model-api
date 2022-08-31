package com.mercadolibre.planning.model.api.domain.service.sla;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OutboundSlaPropertiesService {

  private final GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  private final GetCycleTimeService getCycleTimeService;

  public Map<Instant, SlaProperties> get(final Input input) {
    final List<ZonedDateTime> slas = getSlas(input)
        .stream()
        .map(GetSlaByWarehouseOutput::getDate)
        .map(date -> date.withZoneSameInstant(ZoneOffset.UTC))
        .distinct()
        .collect(Collectors.toList());

    final Map<ZonedDateTime, Long> cycleTimeBySla = getCycleTimeService.execute(new GetCycleTimeInput(input.getLogisticCenterId(), slas));

    return cycleTimeBySla.entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey().toInstant(),
            entry -> new SlaProperties(entry.getValue()))
        );
  }

  private List<GetSlaByWarehouseOutput> getSlas(final Input input) {
    final var dateFrom = DateUtils.fromInstant(input.slaFrom);
    final var dateTo = DateUtils.fromInstant(input.slaTo);
    final var slas = input.getDefaultSlas()
        .stream()
        .map(DateUtils::fromInstant)
        .collect(Collectors.toList());

    final GetSlaByWarehouseInput getSlaByWarehouseInput = new GetSlaByWarehouseInput(
        input.getLogisticCenterId(),
        dateFrom,
        dateTo,
        slas,
        input.getTimeZone()
    );

    return getSlaByWarehouseOutboundService.execute(getSlaByWarehouseInput);
  }

  @Value
  public static class Input {
    String logisticCenterId;

    Workflow workflow;

    Instant slaFrom;

    Instant slaTo;

    List<Instant> defaultSlas;

    String timeZone;
  }

}
