package com.mercadolibre.planning.model.api.domain.usecase.entities;

import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.springframework.util.CollectionUtils;

@Getter
@SuperBuilder
@EqualsAndHashCode
public class GetEntityInput {

  private String warehouseId;

  private Workflow workflow;

  private EntityType entityType;

  private ZonedDateTime dateFrom;

  private ZonedDateTime dateTo;

  private Source source;

  private List<ProcessPath> processPaths;

  private List<ProcessName> processName;

  private List<Simulation> simulations;

  private Instant viewDate;

  public List<String> getProcessNamesAsString() {
    return getProcessName().stream().map(Enum::name).collect(toList());
  }

  public List<ProcessPath> getProcessPaths() {
    return CollectionUtils.isEmpty(processPaths) ? List.of(ProcessPath.GLOBAL) : processPaths;
  }
}
