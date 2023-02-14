package com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get;

import static com.mercadolibre.planning.model.api.util.MathUtil.safeDiv;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ThroughputService {
  private final GetThroughputUseCase getThroughputUseCase;

  private static Map<ProcessPath, Map<ProcessName, Map<Instant, Double>>> asRatios(final List<EntityOutput> throughput) {
    final var totalsByProcessAndDate = throughput.stream()
        .collect(Collectors.groupingBy(
            EntityOutput::getProcessName,
            Collectors.toMap(EntityOutput::getDate, EntityOutput::getQuantity, Double::sum)
        ));

    return throughput.stream()
        .collect(
            Collectors.groupingBy(EntityOutput::getProcessPath,
                Collectors.groupingBy(EntityOutput::getProcessName,
                    Collectors.toMap(
                        entity -> entity.getDate().toInstant(),
                        entity -> safeDiv(
                            entity.getQuantity(),
                            totalsByProcessAndDate.get(entity.getProcessName())
                                .get(entity.getDate())
                        )
                    )
                )
            )
        );
  }

  public Map<ProcessPath, Map<ProcessName, Map<Instant, Double>>> getThroughputRatioByProcessPath(
      final String logisticCenterId,
      final Workflow workflow,
      final List<ProcessPath> processPaths,
      final Set<ProcessName> processes,
      final Instant dateFrom,
      final Instant dateTo,
      final Instant viewDate
  ) {

    final GetEntityInput input = GetEntityInput.builder()
        .warehouseId(logisticCenterId)
        .workflow(workflow)
        .dateFrom(ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC))
        .dateTo(ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC))
        .source(SIMULATION)
        .processName(new ArrayList<>(processes))
        .processPaths(processPaths)
        .simulations(emptyList())
        .viewDate(viewDate)
        .build();

    final var throughputs = getThroughputUseCase.execute(input);

    return asRatios(throughputs);
  }
}
