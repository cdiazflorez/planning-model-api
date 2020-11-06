package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static java.util.Comparator.comparing;

@Service
@AllArgsConstructor
public class GetThroughputEntityUseCase implements GetEntityUseCase {

    private static final Set<ProcessingType> THROUGHPUT_PROCESSING_TYPES =
            Set.of(ProcessingType.ACTIVE_WORKERS);

    private final GetHeadcountEntityUseCase headcountEntityUseCase;
    private final GetProductivityEntityUseCase productivityEntityUseCase;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        final List<EntityOutput> headcounts = new ArrayList<>(headcountEntityUseCase
                .execute(createHeadcountInput(input)));

        final List<EntityOutput> productivities = new ArrayList<>(productivityEntityUseCase
                .execute(createProductivityInput(input)));

        sortByProcessNameAndDate(headcounts, productivities);

        return createThroughputs(headcounts, productivities);
    }

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == THROUGHPUT;
    }

    private List<EntityOutput> createThroughputs(final List<EntityOutput> headcounts,
                                                 final List<EntityOutput> productivities) {

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
                                    .source(headcount.getSource())
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

    private GetEntityInput createProductivityInput(final GetEntityInput input) {
        return new GetEntityInput(input.getWarehouseId(), input.getWorkflow(), PRODUCTIVITY,
                input.getDateFrom(), input.getDateTo(), input.getSource(), input.getProcessName(),
                input.getProcessingType());
    }

    private GetEntityInput createHeadcountInput(final GetEntityInput input) {
        return new GetEntityInput(input.getWarehouseId(), input.getWorkflow(), HEADCOUNT,
                input.getDateFrom(), input.getDateTo(), input.getSource(), input.getProcessName(),
                THROUGHPUT_PROCESSING_TYPES);
    }

}
