package com.mercadolibre.planning.model.api.projection;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.builder.Projector;
import com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult;
import com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult.Sla;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public final class SLAProjectionService {

  private SLAProjectionService() {
  }

  public static SlaProjectionResult execute(
      final Instant executionDateFrom,
      final Instant executionDateTo,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final Map<Instant, Instant> cutOff,
      final Projector projector
  ) {
    final List<Instant> slas = getSLAs(currentBacklog, forecastBacklog);

    final ContextsHolder updatedContext =
        Projection.execute(executionDateFrom, executionDateTo, currentBacklog, forecastBacklog, throughput, projector);

    final SlaProjectionResult projectionResult = projector.calculateProjectedEndDate(slas, updatedContext);

    final List<Sla> slasSorted = projectionResult.slas().stream()
        .sorted(Comparator.comparing(Sla::date))
        .toList();

    return new SlaProjectionResult(slasSorted);
  }

  private static List<Instant> getSLAs(final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
                                       final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog) {
    final List<Instant> forecastSlas = forecastBacklog.values().stream()
        .flatMap(instantMapEntry -> instantMapEntry.values().stream()
            .flatMap(processPathMapEntry -> processPathMapEntry.keySet().stream())
        ).toList();

    final List<Instant> backlogSlas = currentBacklog.values().stream()
        .flatMap(processNameMapEntry -> processNameMapEntry.values().stream()
            .flatMap(pathMapEntry -> pathMapEntry.keySet().stream())
        ).toList();

    return Stream.concat(forecastSlas.stream(), backlogSlas.stream())
        .distinct()
        .toList();
  }
}
