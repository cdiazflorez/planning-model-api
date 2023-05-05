package com.mercadolibre.planning.model.api.domain.entity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_SINGLE_SKU;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProcessNameToProcessPath {

  WAVING(
      ProcessName.WAVING,
      List.of(TOT_MONO, NON_TOT_MONO, TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH, TOT_MULTI_ORDER, NON_TOT_MULTI_ORDER, TOT_SINGLE_SKU)
  ),
  PICKING(
      ProcessName.PICKING,
      List.of(TOT_MONO, NON_TOT_MONO, TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH, TOT_MULTI_ORDER, NON_TOT_MULTI_ORDER, TOT_SINGLE_SKU)
  ),
  PACKING(
      ProcessName.PACKING,
      List.of(TOT_MONO, NON_TOT_MONO, TOT_MULTI_ORDER, NON_TOT_MULTI_ORDER, TOT_SINGLE_SKU)
  ),
  BATCH_SORTER(
      ProcessName.BATCH_SORTER,
      List.of(TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH)
  ),
  WALL_IN(
      ProcessName.WALL_IN,
      List.of(TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH)
  ),
  PACKING_WALL(
      ProcessName.PACKING_WALL,
      List.of(TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH)
  );

  private static final Map<String, ProcessNameToProcessPath> LOOKUP = Arrays.stream(values()).collect(
      toMap(ProcessNameToProcessPath::toString, Function.identity())
  );

  private final ProcessName process;

  private final List<ProcessPath> paths;

  public static List<ProcessNameToProcessPath> getTriggersProcess() {
    return Arrays.stream(ProcessNameToProcessPath.values())
        .filter(processNameToProcessPath -> !processNameToProcessPath.getProcess().equals(ProcessName.WAVING))
        .filter(processNameToProcessPath -> !processNameToProcessPath.getProcess().equals(ProcessName.PICKING))
        .collect(Collectors.toUnmodifiableList());
  }

}
