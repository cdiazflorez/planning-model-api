package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;

@Service
@AllArgsConstructor
public abstract class GetThroughputEntityUseCase implements GetEntityUseCase {

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == THROUGHPUT;
    }

    protected List<EntityOutput> createThroughputs(final List<EntityOutput> headcounts,
                                                   final List<EntityOutput> productivities) {

        final Map<ProcessName, Map<ZonedDateTime, EntityOutput>> headcountsMap =
                toMap(headcounts);
        final Map<ProcessName, Map<ZonedDateTime, EntityOutput>> productivityMap =
                toMap(productivities);

        final List<EntityOutput> throughput = new ArrayList<>();

        headcountsMap.forEach((processName, headcountsByDateTime) -> {
            headcountsByDateTime.forEach((dateTime, headcount) -> {

                final EntityOutput productivity = productivityMap.get(processName).get(dateTime);

                if (productivity != null) {
                    throughput.add(EntityOutput.builder()
                            .workflow(headcount.getWorkflow())
                            .date(headcount.getDate().withFixedOffsetZone())
                            .source(getDefinitiveSource(headcount, productivity))
                            .processName(headcount.getProcessName())
                            .metricUnit(UNITS_PER_HOUR)
                            .value(headcount.getValue() * productivity.getValue())
                            .build());
                }
            });
        });
        sortByProcessNameAndDate(throughput);
        return throughput;
    }

    private Map<ProcessName, Map<ZonedDateTime, EntityOutput>> toMap(
            final List<EntityOutput> entities) {

        return entities.stream().collect(Collectors.groupingBy(
                EntityOutput::getProcessName,
                Collectors.toMap(
                        o -> o.getDate().withFixedOffsetZone(),
                        Function.identity(),
                        (e1, e2) -> SIMULATION == e1.getSource() ? e1 : e2
                )));
    }

    private void sortByProcessNameAndDate(final List<EntityOutput> throughput) {
        final Comparator<EntityOutput> compareByProcessNameAndDate = Comparator
                .comparing(EntityOutput::getProcessName)
                .thenComparing(EntityOutput::getDate);

        throughput.sort(compareByProcessNameAndDate);
    }

    private Source getDefinitiveSource(final EntityOutput headcount,
                                       final EntityOutput productivity) {
        return headcount.getSource() == SIMULATION || productivity.getSource() == SIMULATION
                ? SIMULATION
                : FORECAST;
    }
}
