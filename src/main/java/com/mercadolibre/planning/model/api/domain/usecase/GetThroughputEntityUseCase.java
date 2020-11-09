package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.util.Comparator.comparing;

@Service
@AllArgsConstructor
public abstract class GetThroughputEntityUseCase implements GetEntityUseCase {

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == THROUGHPUT;
    }

    protected List<EntityOutput> createThroughputs(final List<EntityOutput> headcounts,
                                                   final List<EntityOutput> productivities) {

        sortByProcessNameAndDate(headcounts, productivities);

        final List<EntityOutput> throughputs = new ArrayList<>();
        if (headcounts.size() == productivities.size()) {
            IntStream.range(0, headcounts.size())
                    .forEach(i -> {
                        final EntityOutput headcount = headcounts.get(i);
                        final EntityOutput productivity = productivities.get(i);
                        if (headcount.getProcessName() == productivity.getProcessName()) {
                            throughputs.add(EntityOutput.builder()
                                    .workflow(headcount.getWorkflow())
                                    .date(headcount.getDate())
                                    .source(getDefinitiveSource(headcount, productivity))
                                    .processName(headcount.getProcessName())
                                    .metricUnit(UNITS_PER_HOUR)
                                    .value(headcount.getValue() * productivity.getValue())
                                    .build());
                        }
                    });
        }

        return throughputs;
    }

    private void sortByProcessNameAndDate(final List<EntityOutput> headcounts,
                                          final List<EntityOutput> productivities) {
        final Comparator<EntityOutput> compareByProcessNameAndDate =
                comparing(EntityOutput::getProcessName).thenComparing(EntityOutput::getDate);

        headcounts.sort(compareByProcessNameAndDate);
        productivities.sort(compareByProcessNameAndDate);
    }

    private Source getDefinitiveSource(final EntityOutput headcount,
                                       final EntityOutput productivity) {
        return headcount.getSource() == SIMULATION || productivity.getSource() == SIMULATION
                ? SIMULATION
                : FORECAST;
    }
}
