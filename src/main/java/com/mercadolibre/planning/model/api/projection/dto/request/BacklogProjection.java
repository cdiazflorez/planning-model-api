package com.mercadolibre.planning.model.api.projection.dto.request;

import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class BacklogProjection {

    Backlog backlog;

    @JsonProperty("planned_unit")
    PlannedUnit plannedUnit;

    List<Throughput> throughput;

    @NotNull
    @JsonProperty("date_from")
    Instant dateFrom;

    @NotNull
    @JsonProperty("date_to")
    Instant dateTo;

    Workflow workflow;

    public Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> mapBacklogs() {
        return this.backlog.getProcess().stream()
            .collect(groupingBy(
                Process::getName,
                mapping(
                    Process::getProcessPath,
                    flatMapping(List::stream,
                        groupingBy(Process.ProcessPathDetail::getName,
                            mapping(Process.ProcessPathDetail::getQuantity,
                                flatMapping(List::stream,
                                    toMap(Process.QuantityByDate::getDateOut,
                                        quantityByDate -> (long) quantityByDate.getTotal(),
                                        Long::sum
                                    ))
                            )))
                )
            ));
    }

    public Map<ProcessName, Map<Instant, Long>> mapThroughput() {
      return this.throughput.stream().flatMap(tph -> tph.getQuantityByProcessName().stream()
              .map(quantityByProcessName -> new AbstractMap.SimpleEntry<>(tph.getOperationHour(), quantityByProcessName)))
          .collect(groupingBy(
              a -> a.getValue().getName(),
              groupingBy(
                  AbstractMap.SimpleEntry::getKey,
                  summingLong(entry -> (long) entry.getValue().getTotal())
              )
          ));
    }

    public Map<Instant, Map<ProcessPath, Map<Instant, Long>>> mapForecast() {
      return this.plannedUnit.getProcessPath().stream().flatMap(processPathRequest -> processPathRequest.getQuantity().stream()
              .map(quantity -> new AbstractMap.SimpleEntry<>(quantity.getDateIn(),
                  new AbstractMap.SimpleEntry<>(processPathRequest.getName(),
                      new AbstractMap.SimpleEntry<>(quantity.getDateOut(), (long) quantity.getTotal())
              ))))
          .collect(groupingBy(
              AbstractMap.SimpleEntry::getKey,
              groupingBy(
                  entry -> entry.getValue().getKey(),
                  toMap(
                      entry -> entry.getValue().getValue().getKey(),
                      entry -> entry.getValue().getValue().getValue(),
                      (total1, total2) -> total1, TreeMap::new
                  )
              )
          ));
    }

    @Value
    static class ProcessPathByDateIn {
      Instant dateIn;
      ProcessPathRequest processPathRequest;
  }
}
