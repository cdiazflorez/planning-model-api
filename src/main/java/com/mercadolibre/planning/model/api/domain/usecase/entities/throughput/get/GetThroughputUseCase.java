package com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get;

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
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.toMapByProcessNameAndDate;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.toMapByProcessNameDateAndSource;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.stream.Collectors.toList;

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

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        List<EntityOutput> allThroughputs = new ArrayList<>();

        // Esto es temporal hasta que agreguen la columna de Reps Sistemicos de Receiving al forecast
        if (input.getProcessName().contains(ProcessName.RECEIVING)) {
            allThroughputs = headcountEntityUseCase.execute(createReceivingTph(input));
        }

        final List<EntityOutput> headcounts = headcountEntityUseCase.execute(
                createHeadcountInput(input));

        final List<ProductivityOutput> productivity = productivityEntityUseCase.execute(
                createProductivityInput(input));

        allThroughputs.addAll(createThroughput(headcounts, productivity, input.getWorkflow()));
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

    private List<EntityOutput> createThroughput(final List<EntityOutput> headcounts,
                                                final List<ProductivityOutput> productivity,
                                                final Workflow workflow) {

        final Map<ProcessName, Map<ZonedDateTime, Map<Source, EntityOutput>>> headcountsMap =
                toMapByProcessNameDateAndSource(headcounts);

        final Map<ProcessName, Map<ZonedDateTime, Map<Source, EntityOutput>>> productivityMap =
                toMapByProcessNameDateAndSource(productivity.stream()
                        .filter(ProductivityOutput::isMainProductivity)
                        .collect(toList()));

        final Map<ProcessName, Map<ZonedDateTime, EntityOutput>> polyvalentProductivityMap =
                toMapByProcessNameAndDate(productivity.stream()
                        .filter(ProductivityOutput::isPolyvalentProductivity)
                        .collect(toList()));

        final List<EntityOutput> throughput = new ArrayList<>();

        headcountsMap.forEach((processName, headcountsByDateTime) -> headcountsByDateTime.forEach(
                (dateTime, headcountBySource) -> {

                    final Map<Source, EntityOutput> productivityBySource =
                            productivityMap.get(processName).get(dateTime);

                    if (!CollectionUtils.isEmpty(productivityBySource)) {
                        final long tph;
                        final EntityOutput headcount = headcountBySource.get(FORECAST);
                        final EntityOutput currentProductivity = productivityBySource
                                .getOrDefault(SIMULATION, productivityBySource.get(FORECAST));

                        final EntityOutput simulatedHeadcount = headcountBySource.get(SIMULATION);
                        if (simulatedHeadcount == null) {
                            tph = headcount.getValue() * currentProductivity.getValue();
                        } else {
                            final long currentPolyvalentProductivity = Optional.ofNullable(
                                            polyvalentProductivityMap.get(processName)
                                    )
                                    .flatMap(polivalentProductivity ->
                                            Optional.ofNullable(polivalentProductivity.get(dateTime))
                                    )
                                    .map(EntityOutput::getValue)
                                    .orElse(0L);

                            tph = calculateTphValue(
                                    headcount == null ? 0 : headcount.getValue(),
                                    simulatedHeadcount.getValue(),
                                    currentProductivity.getValue(),
                                    currentPolyvalentProductivity
                            );
                        }
                        throughput.add(EntityOutput.builder()
                                .workflow(workflow)
                                .date(dateTime)
                                .source(currentProductivity.getSource())
                                .processName(processName)
                                .metricUnit(UNITS_PER_HOUR)
                                .value(tph)
                                .build());
                    }
                }));
        return throughput;
    }

    private Long calculateTphValue(final long forecastHeadcountValue,
                                   final long simulatedHeadcountValue,
                                   final long productivityValue,
                                   final long multiFunctionalPdtValue) {

        final long valueDifference = simulatedHeadcountValue - forecastHeadcountValue;

        if (valueDifference > 0) {
            return valueDifference * multiFunctionalPdtValue
                    + forecastHeadcountValue * productivityValue;
        } else {
            return simulatedHeadcountValue * productivityValue;
        }
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
