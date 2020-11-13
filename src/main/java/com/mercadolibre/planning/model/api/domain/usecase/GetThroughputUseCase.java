package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.util.EntitiesUtil;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;

@Service
@AllArgsConstructor
public class GetThroughputUseCase implements GetEntityUseCase {

    private static final Set<ProcessingType> THROUGHPUT_PROCESSING_TYPES =
            Set.of(ProcessingType.ACTIVE_WORKERS);

    private final GetHeadcountEntityUseCase headcountEntityUseCase;
    private final GetProductivityEntityUseCase productivityEntityUseCase;

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == THROUGHPUT;
    }

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        final List<EntityOutput> headcounts = headcountEntityUseCase.execute(
                createHeadcountInput(input));
        final List<EntityOutput> productivity = productivityEntityUseCase.execute(
                createProductivityInput(input, 1, SIMULATION));
        final List<EntityOutput> polyvalentProductivity = productivityEntityUseCase.execute(
                createProductivityInput(input, 2, FORECAST));

        return createThroughput(headcounts, productivity, polyvalentProductivity);
    }

    private List<EntityOutput> createThroughput(
            final List<EntityOutput> headcounts,
            final List<EntityOutput> productivity,
            final List<EntityOutput> polyvalentProductivity) {

        final Map<ProcessName, Map<ZonedDateTime, Map<Source, EntityOutput>>> headcountsMap =
                EntitiesUtil.toMapByProcessNameDateAndSource(headcounts);
        final Map<ProcessName, Map<ZonedDateTime, Map<Source, EntityOutput>>> productivityMap =
                EntitiesUtil.toMapByProcessNameDateAndSource(productivity);
        final Map<ProcessName, Map<ZonedDateTime, EntityOutput>> polyvalentProductivityMap =
                EntitiesUtil.toMapByProcessNameAndDate(polyvalentProductivity);

        final List<EntityOutput> throughput = new ArrayList<>();

        headcountsMap.forEach((processName, headcountsByDateTime) -> {
            headcountsByDateTime.forEach((dateTime, headcountBySource) -> {

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
                        final EntityOutput currentPolivalentProductivity =
                                polyvalentProductivityMap.get(processName).get(dateTime);

                        tph = calculateTphValue(
                                headcount.getValue(),
                                simulatedHeadcount.getValue(),
                                currentProductivity.getValue(),
                                currentPolivalentProductivity.getValue());
                    }
                    throughput.add(EntityOutput.builder()
                            .workflow(headcount.getWorkflow())
                            .date(headcount.getDate().withFixedOffsetZone())
                            .source(currentProductivity.getSource())
                            .processName(headcount.getProcessName())
                            .metricUnit(UNITS_PER_HOUR)
                            .value(tph)
                            .build());
                }
            });
        });
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

    private GetEntityInput createProductivityInput(final GetEntityInput input,
                                                   final Integer abilityLevel,
                                                   final Source source) {
        return GetEntityInput.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .entityType(PRODUCTIVITY)
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .source(source)
                .processName(input.getProcessName())
                .processingType(input.getProcessingType())
                .simulations(input.getSimulations())
                .abilityLevel(Set.of(abilityLevel))
                .build();

    }

    private GetEntityInput createHeadcountInput(final GetEntityInput input) {
        return GetEntityInput.builder()
                .warehouseId(input.getWarehouseId())
                .workflow(input.getWorkflow())
                .entityType(HEADCOUNT)
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .source(input.getSource())
                .processName(input.getProcessName())
                .processingType(THROUGHPUT_PROCESSING_TYPES)
                .simulations(input.getSimulations())
                .build();
    }
}
