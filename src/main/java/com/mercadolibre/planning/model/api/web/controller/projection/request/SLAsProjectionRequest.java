package com.mercadolibre.planning.model.api.web.controller.projection.request;

import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public record SLAsProjectionRequest(
    Workflow workflow,
    @JsonProperty("date_from")
    Instant dateFrom,
    @JsonProperty("date_to")
    Instant dateTo,
    Backlog backlog,
    @JsonProperty("planned_unit")
    PlannedUnit plannedUnit,
    Set<Throughput> throughput,
    @JsonProperty("cycle_time_by_sla")
    Map<Instant, Integer> cycleTimeBySla
) {

  public Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> mapBacklogs() {
    return backlog.process.stream()
        .collect(
            groupingBy(
                Backlog.Process::name,
                mapping(
                    Backlog.Process::processPath,
                    flatMapping(
                        Collection::stream,
                        groupingBy(
                            Backlog.Process.ProcessPathByDateOut::name,
                            mapping(
                                Backlog.Process.ProcessPathByDateOut::quantityByDate,
                                flatMapping(
                                    Collection::stream,
                                    toMap(
                                        Backlog.Process.ProcessPathByDateOut.QuantityByDateOut::dateOut,
                                        Backlog.Process.ProcessPathByDateOut.QuantityByDateOut::total,
                                        Long::sum
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
  }

  public Map<ProcessName, Map<Instant, Integer>> mapThroughput() {
    return throughput.stream()
        .flatMap(
            tph -> tph.quantityByProcessName().stream()
                .map(
                    quantityByProcessName -> new AbstractMap.SimpleEntry<>(tph.operationHour(), quantityByProcessName))
        )
        .collect(
            groupingBy(
                a -> a.getValue().name(),
                groupingBy(
                    AbstractMap.SimpleEntry::getKey,
                    summingInt(entry -> entry.getValue().total())
                )
            )
        );
  }

  public Map<Instant, Map<ProcessPath, Map<Instant, Long>>> mapForecast() {
    return plannedUnit.processPath.stream()
        .flatMap(
            processPathRequest -> processPathRequest.quantity().stream()
                .map(
                    quantity -> new AbstractMap.SimpleEntry<>(quantity.dateIn(),
                        new AbstractMap.SimpleEntry<>(processPathRequest.name(),
                            new AbstractMap.SimpleEntry<>(quantity.dateOut(), quantity.total())
                        )
                    )
                )
        )
        .collect(
            groupingBy(
                AbstractMap.SimpleEntry::getKey,
                groupingBy(
                    entry -> entry.getValue().getKey(),
                    toMap(
                        entry -> entry.getValue().getValue().getKey(),
                        entry -> entry.getValue().getValue().getValue()
                    )
                )
            )
        );
  }

  public record Backlog(
      Set<Process> process
  ) {
    public record Process(
        ProcessName name,
        @JsonProperty("process_path")
        Set<ProcessPathByDateOut> processPath
    ) {
      public record ProcessPathByDateOut(
          ProcessPath name,
          @JsonProperty("quantity")
          Set<QuantityByDateOut> quantityByDate
      ) {
        public record QuantityByDateOut(
            @JsonProperty("date_out")
            Instant dateOut,
            Long total
        ) {
        }
      }
    }
  }

  public record PlannedUnit(
      @JsonProperty("process_path")
      Set<ProcessPathByDateInOut> processPath
  ) {
    public record ProcessPathByDateInOut(
        ProcessPath name,
        Set<QuantityByDateInOut> quantity
    ) {
      public record QuantityByDateInOut(
          @JsonProperty("date_in")
          Instant dateIn,
          @JsonProperty("date_out")
          Instant dateOut,
          Long total
      ) {
      }
    }
  }

  public record Throughput(
      @JsonProperty("operation_hour")
      Instant operationHour,
      @JsonProperty("quantity_by_process_name")
      Set<QuantityByProcessName> quantityByProcessName
  ) {
    public record QuantityByProcessName(
        ProcessName name,
        Integer total
    ) {
    }
  }
}
