package com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.PolyvalenteProductivityUtils.calculatePolyvalentProductivityRatioByProcessPath;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.toMapByProcessPathProcessNameAndDate;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.toMapByProcessPathProcessNameDateAndSource;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * The throughput per hour is defined as the quantity of items that can be processed by a process,
 * it is calculated as the headcount times the productivity.
 *
 * <p>
 * Both of these inputs are retrieved from the forecast and updated with the values from stored simulations or unnaplied simulations.
 * </p>
 */
@Service
@AllArgsConstructor
public class GetThroughputUseCase implements EntityUseCase<GetEntityInput, List<EntityOutput>> {

  private static final int MAIN_ABILITY = 1;

  private static final int POLYVALENT_ABILITY = 2;

  private final GetHeadcountEntityUseCase headcountEntityUseCase;

  private final GetProductivityEntityUseCase productivityEntityUseCase;

  @Override
  public boolean supportsEntityType(final EntityType entityType) {
    return entityType == THROUGHPUT;
  }

  /**
   * Calculates the productivity for each of the specified processes.
   */
  @Override
  public List<EntityOutput> execute(final GetEntityInput input) {
    List<EntityOutput> allThroughputs = new ArrayList<>();

    // Esto es temporal hasta que agreguen la columna de Reps Sistemicos de Receiving al forecast
    if (input.getProcessName().contains(ProcessName.RECEIVING)) {
      allThroughputs = headcountEntityUseCase.execute(createReceivingTph(input));
    }

    final List<EntityOutput> headcounts = headcountEntityUseCase.execute(createHeadcountInput(input));
    final List<ProductivityOutput> productivity = productivityEntityUseCase.execute(createProductivityInput(input));

    allThroughputs.addAll(createThroughput(
            input.getProcessPaths(),
            input.getProcessName(),
            headcounts,
            productivity,
            input.getWorkflow(),
            input.getSource()
        )
    );
    return allThroughputs;
  }

  private GetHeadcountInput createReceivingTph(final GetEntityInput input) {
    return GetHeadcountInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .entityType(THROUGHPUT)
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .source(input.getSource())
        .processName(List.of(ProcessName.RECEIVING))
        .viewDate(input.getViewDate())
        .build();
  }

  private List<EntityOutput> createThroughput(
      final List<ProcessPath> processPaths,
      final List<ProcessName> processes,
      final List<EntityOutput> headcounts,
      final List<ProductivityOutput> productivity,
      final Workflow workflow,
      final Source source
  ) {

    final var headcountsMap = toMapByProcessPathProcessNameDateAndSource(headcounts);

    final var regularProductivityMap = toMapByProcessPathProcessNameDateAndSource(productivity.stream()
        .filter(ProductivityOutput::isMainProductivity)
        .collect(toList()));

    // Despite the `current_headcount_productivity` table has the `ability_level` field, currently (2022-08-26) it contains no polyvalent
    // productivity information. Therefore, this map contains polivalent productivity from the forecast only.
    final var polyvalentProductivityMap = toMapByProcessPathProcessNameAndDate(
        productivity.stream()
            .filter(ProductivityOutput::isPolyvalentProductivity)
    );

    final PolyvalentProductivityRatio polyvalentProductivityRatios = source == FORECAST
        ? PolyvalentProductivityRatio.empty()
        : calculatePolyvalentProductivityRatioByProcessPath(regularProductivityMap, polyvalentProductivityMap);

    return processPaths.stream()
        .flatMap(processPath ->
            processes.stream()
                // receiving is filtered as it has its own way of calculating its tph
                .filter(process -> process != ProcessName.RECEIVING)
                .flatMap(process ->
                    createThroughput(
                        workflow,
                        processPath,
                        process,
                        headcountsMap,
                        regularProductivityMap,
                        polyvalentProductivityRatios
                    ).stream()
                )
        ).collect(toList());
  }

  private List<EntityOutput> createThroughput(
      final Workflow workflow,
      final ProcessPath processPath,
      final ProcessName process,
      final Map<ProcessPath, Map<ProcessName, Map<ZonedDateTime, Map<Source, EntityOutput>>>> headcount,
      final Map<ProcessPath, Map<ProcessName, Map<ZonedDateTime, Map<Source, ProductivityOutput>>>> regularProductivity,
      final PolyvalentProductivityRatio polyvalentProductivityRatios
  ) {
    final var headcountOpt = Optional.of(headcount)
        .map(hm -> hm.get(processPath))
        .map(hm -> hm.get(process));

    final var productivityOpt = Optional.of(regularProductivity)
        .map(rp -> rp.get(processPath))
        .map(rp -> rp.get(process));

    if (headcountOpt.isEmpty() || productivityOpt.isEmpty()) {
      return Collections.emptyList();
    }

    return createThroughputs(
        workflow,
        processPath,
        process,
        headcountOpt.get(),
        productivityOpt.get(),
        polyvalentProductivityRatios
    );
  }

  private List<EntityOutput> createThroughputs(
      final Workflow workflow,
      final ProcessPath processPath,
      final ProcessName process,
      final Map<ZonedDateTime, Map<Source, EntityOutput>> headcount,
      final Map<ZonedDateTime, Map<Source, ProductivityOutput>> regularProductivity,
      final PolyvalentProductivityRatio polyvalentProductivityRatios
  ) {
    return headcount.keySet()
        .stream()
        .map(dateTime -> calculate(dateTime, workflow, processPath, process, headcount, regularProductivity, polyvalentProductivityRatios))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  private Optional<EntityOutput> calculate(
      final ZonedDateTime dateTime,
      final Workflow workflow,
      final ProcessPath processPath,
      final ProcessName process,
      final Map<ZonedDateTime, Map<Source, EntityOutput>> headcountsBySourceByDate,
      final Map<ZonedDateTime, Map<Source, ProductivityOutput>> regularProductivityBySourceByDate,
      final PolyvalentProductivityRatio polyvalentProductivityRatios
  ) {
    final Map<Source, EntityOutput> headcountBySource = headcountsBySourceByDate.get(dateTime);
    final Map<Source, ProductivityOutput> regularProductivityBySource = regularProductivityBySourceByDate.get(dateTime);
    if (CollectionUtils.isEmpty(headcountBySource) || CollectionUtils.isEmpty(regularProductivityBySource)) {
      return Optional.empty();
    }

    final var forecastHeadcount = headcountBySource.get(FORECAST);
    final var forecastRegularProductivity = regularProductivityBySource.get(FORECAST);
    if (forecastHeadcount == null || forecastRegularProductivity == null) {
      return Optional.empty();
    }
    final var simulatedHeadcount = headcountBySource.getOrDefault(SIMULATION, forecastHeadcount);
    final var simulatedRegularProductivity = regularProductivityBySource.getOrDefault(SIMULATION, forecastRegularProductivity);

    final var forecastPolyvalentProductivity =
        polyvalentProductivityRatios.getForProcessPathProcessNameAndDate(processPath, process, dateTime);

    final double tph;
    if (simulatedHeadcount.getQuantity() <= forecastHeadcount.getQuantity()
        || workflow != Workflow.FBM_WMS_OUTBOUND
        || forecastPolyvalentProductivity.isEmpty()
    ) {
      tph = simulatedHeadcount.getQuantity() * simulatedRegularProductivity.getQuantity();
    } else {
      final var regularHeadcount = forecastHeadcount.getQuantity();
      final var regularProductivity = simulatedRegularProductivity.getQuantity();
      final var polyvalentHeadcount = simulatedHeadcount.getQuantity() - regularHeadcount;
      final double polyvalentProductivity = simulatedRegularProductivity.getQuantity() * forecastPolyvalentProductivity.get();
      tph = regularHeadcount * regularProductivity + polyvalentHeadcount * polyvalentProductivity;
    }

    return Optional.of(
        EntityOutput.builder()
            .workflow(workflow)
            .processPath(processPath)
            .processName(process)
            .date(dateTime)
            .source(simulatedRegularProductivity.getSource())
            .metricUnit(UNITS_PER_HOUR)
            .quantity(tph)
            .build()
    );
  }

  private GetProductivityInput createProductivityInput(final GetEntityInput input) {
    return GetProductivityInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .entityType(PRODUCTIVITY)
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .source(input.getSource())
        .processPaths(input.getProcessPaths())
        .processName(input.getProcessName())
        .simulations(input.getSimulations())
        .abilityLevel(Set.of(MAIN_ABILITY, POLYVALENT_ABILITY))
        .viewDate(input.getViewDate())
        .build();
  }

  private GetHeadcountInput createHeadcountInput(final GetEntityInput input) {
    return GetHeadcountInput.builder()
        .warehouseId(input.getWarehouseId())
        .workflow(input.getWorkflow())
        .entityType(HEADCOUNT)
        .dateFrom(input.getDateFrom())
        .dateTo(input.getDateTo())
        .source(input.getSource())
        .processPaths(input.getProcessPaths())
        .processName(input.getProcessName())
        .simulations(input.getSimulations())
        .processingType(Set.of(ProcessingType.ACTIVE_WORKERS))
        .viewDate(input.getViewDate())
        .build();
  }
}
