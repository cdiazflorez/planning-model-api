package com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Value
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class GetProductivityInput extends GetEntityInput {

  Set<Integer> abilityLevel;

  List<ProcessPath> processPaths;

  public List<ProcessPath> getProcessPaths() {
      return  processPaths == null || processPaths.isEmpty()
              ? List.of(GLOBAL)
              : processPaths;
  }

  public List<String> getProcessPathsAsString() {
    return getProcessPaths().stream().map(Enum::name).collect(Collectors.toList());
  }

  public Instant viewDate() {
    return super.getViewDate() == null ? Instant.now() : super.getViewDate();
  }
}
