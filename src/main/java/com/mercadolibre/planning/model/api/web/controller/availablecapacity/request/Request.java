package com.mercadolibre.planning.model.api.web.controller.availablecapacity.request;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class Request {
  @NotNull Instant executionDateFrom;
  @NotNull Instant executionDateTo;
  @NotNull List<UnitsByProcessPathAndProcess> currentBacklogAtViewDate;
  List<UnitsByProcessPathDateInAndDateOut> forecastBacklog;
  @NotNull Map<ProcessName, Map<Instant, Integer>> throughput;
  @NotNull Map<Instant, Integer> cycleTimeBySla;

  public Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> asCurrentBacklog() {
    return currentBacklogAtViewDate.stream()
        .collect(groupingBy(
                UnitsByProcessPathAndProcess::getProcessName,
                groupingBy(
                    UnitsByProcessPathAndProcess::getProcessPath,
                    toMap(
                        UnitsByProcessPathAndProcess::getDateOut,
                        backlog -> (long) backlog.getUnits(),
                        Long::sum
                    )
                )
            )
        );
  }

  public Map<Instant, Map<ProcessPath, Map<Instant, Long>>> asForecastBacklog() {
    if (forecastBacklog == null) {
      return emptyMap();
    }
    return forecastBacklog.stream()
        .collect(groupingBy(
                UnitsByProcessPathDateInAndDateOut::getDateIn,
                groupingBy(
                    UnitsByProcessPathDateInAndDateOut::getProcessPath,
                    toMap(
                        UnitsByProcessPathDateInAndDateOut::getDateOut,
                        backlog -> backlog.units.longValue(),
                        Long::sum
                    )
                )
            )
        );
  }

  @Value
  public static class UnitsByProcessPathDateInAndDateOut {
    ProcessPath processPath;
    Instant dateIn;
    Instant dateOut;
    Float units;
  }
}
