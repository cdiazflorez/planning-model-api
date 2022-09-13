package com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.toMapByProcessNameAndDate;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.toMapByProcessNameDateAndSource;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
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
import java.util.Objects;
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
 *   Both of these inputs are retrieved from the forecast and updated with the values from stored simulations or unnaplied simulations.
 * </p>
 *
 * */
@Service
@AllArgsConstructor
@SuppressWarnings("PMD.ExcessiveImports")
public class GetThroughputUseCase
    implements EntityUseCase<GetEntityInput, List<EntityOutput>> {

  private static final int MAIN_ABILITY = 1;

  private static final int POLYVALENT_ABILITY = 2;

  private final GetHeadcountEntityUseCase headcountEntityUseCase;

  private final GetProductivityEntityUseCase productivityEntityUseCase;

  @Override
  public boolean supportsEntityType(final EntityType entityType) {
    return entityType == THROUGHPUT;
  }

  /** Calculates the productivity for each of the specified processes. */
  @Override
  public List<EntityOutput> execute(final GetEntityInput input) {
    List<EntityOutput> allThroughputs = new ArrayList<>();

    // Esto es temporal hasta que agreguen la columna de Reps Sistemicos de Receiving al forecast
    if (input.getProcessName().contains(ProcessName.RECEIVING)) {
      allThroughputs = headcountEntityUseCase.execute(createReceivingTph(input));
    }

    final List<EntityOutput> headcounts = headcountEntityUseCase.execute(createHeadcountInput(input));
    final List<ProductivityOutput> productivity = productivityEntityUseCase.execute(createProductivityInput(input));

    allThroughputs.addAll(createThroughput(input.getProcessName(), headcounts, productivity, input.getWorkflow()));
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
        .build();
  }

  private List<EntityOutput> createThroughput(final List<ProcessName> processes,
                                              final List<EntityOutput> headcounts,
                                              final List<ProductivityOutput> productivity,
                                              final Workflow workflow) {

    final Map<ProcessName, Map<ZonedDateTime, Map<Source, EntityOutput>>> headcountsMap =
        toMapByProcessNameDateAndSource(headcounts);

    final Map<ProcessName, Map<ZonedDateTime, Map<Source, ProductivityOutput>>> regularProductivityMap =
        toMapByProcessNameDateAndSource(productivity.stream()
            .filter(ProductivityOutput::isMainProductivity)
            .collect(toList()));

    // Despite the `current_headcount_productivity` table has the `ability_level` field, currently (2022-08-26) it contains no polyvalent
    // productivity information. Therefore, this map contains polivalent productivity from the forecast only.
    final Map<ProcessName, Map<ZonedDateTime, ProductivityOutput>> polyvalentProductivityMap =
        toMapByProcessNameAndDate(productivity.stream().filter(ProductivityOutput::isPolyvalentProductivity));

    // receiving is filtered as it has its own way of calculating its tph
      return processes.stream()
          .filter(process -> process != ProcessName.RECEIVING)
          .flatMap(process ->
              createThroughputs(
              workflow,
              process,
              headcountsMap.get(process),
              regularProductivityMap.get(process),
              polyvalentProductivityMap.get(process)
          ).stream())
          .collect(toList());
  }

  private List<EntityOutput> createThroughputs(final Workflow workflow,
                                               final ProcessName process,
                                               final Map<ZonedDateTime, Map<Source, EntityOutput>> headcount,
                                               final Map<ZonedDateTime, Map<Source, ProductivityOutput>> regularProductivity,
                                               final Map<ZonedDateTime, ProductivityOutput> polyvalentProductivity) {

    if (Objects.isNull(headcount) || Objects.isNull(regularProductivity)) {
      return Collections.emptyList();
    }

    return headcount.keySet()
        .stream()
        .map(dateTime -> calculate(dateTime, workflow, process, headcount, regularProductivity, polyvalentProductivity))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toList());
  }

  private Optional<EntityOutput> calculate(final ZonedDateTime dateTime,
                                           final Workflow workflow,
                                           final ProcessName process,
                                           final Map<ZonedDateTime, Map<Source, EntityOutput>> headcountsBySourceByDate,
                                           final Map<ZonedDateTime, Map<Source, ProductivityOutput>> regularProductivityBySourceByDate,
                                           final Map<ZonedDateTime, ProductivityOutput> forecastPolyvalentProductivityByDate
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
    final var forecastPolyvalentProductivity = Optional.ofNullable(forecastPolyvalentProductivityByDate)
        .flatMap(pp -> Optional.ofNullable(pp.get(dateTime)))
        .map(EntityOutput::getValue);

    final long tph;
    if (simulatedHeadcount.getValue() <= forecastHeadcount.getValue()
        || workflow != Workflow.FBM_WMS_OUTBOUND
        || forecastPolyvalentProductivity.isEmpty()
    ) {
        tph = simulatedHeadcount.getValue() * simulatedRegularProductivity.getValue();
    } else {
        final var regularHeadcount = forecastHeadcount.getValue();
        final var regularProductivity = simulatedRegularProductivity.getValue();
        final var polyvalentHeadcount = simulatedHeadcount.getValue() - regularHeadcount;
        final double polyvalentProductivityRatio = forecastPolyvalentProductivity.get() / (double) forecastRegularProductivity.getValue();
        final double polyvalentProductivity = simulatedRegularProductivity.getValue() * polyvalentProductivityRatio;
        tph = regularHeadcount * regularProductivity + Math.round(polyvalentHeadcount * polyvalentProductivity);
    }

    return Optional.of(
        EntityOutput.builder()
            .workflow(workflow)
            .date(dateTime)
            .source(simulatedRegularProductivity.getSource())
            .processName(process)
            .metricUnit(UNITS_PER_HOUR)
            .value(tph)
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
        .processName(input.getProcessName())
        .simulations(input.getSimulations())
        .abilityLevel(Set.of(MAIN_ABILITY, POLYVALENT_ABILITY))
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
        .processName(input.getProcessName())
        .simulations(input.getSimulations())
        .processingType(Set.of(ProcessingType.ACTIVE_WORKERS))
        .build();
  }
}
