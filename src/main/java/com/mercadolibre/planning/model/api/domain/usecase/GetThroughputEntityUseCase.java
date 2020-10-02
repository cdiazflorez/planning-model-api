package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.HeadcountOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.ThroughputOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;

@Service
@AllArgsConstructor
public class GetThroughputEntityUseCase implements GetEntityUseCase {

    private final GetHeadcountEntityUseCase headcountEntityUseCase;
    private final GetProductivityEntityUseCase productivityEntityUseCase;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        if (input.getSource() == null || input.getSource() == FORECAST) {
            return getForecastThroughput(input);
        } else {
            return getSimulationThroughput();
        }
    }

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == THROUGHPUT;
    }

    private List<EntityOutput> getForecastThroughput(final GetEntityInput input) {
        final List<EntityOutput> headcounts = new ArrayList<>(headcountEntityUseCase
                .execute(createHeadcountInput(input)));

        final List<EntityOutput> productivities = new ArrayList<>(productivityEntityUseCase
                .execute(createProductivityInput(input)));

        sortByProcessNameAndDate(headcounts, productivities);

        return createThroughputs(headcounts, productivities);
    }

    private List<EntityOutput> createThroughputs(final List<EntityOutput> headcounts,
                                                 final List<EntityOutput> productivities) {

        final List<EntityOutput> throughputs = new ArrayList<>();
        if (headcounts.size() == productivities.size()) {
            IntStream.range(0, headcounts.size())
                    .forEach(i -> {
                        final HeadcountOutput headcount = (HeadcountOutput) headcounts.get(i);
                        final ProductivityOutput productivity =
                                (ProductivityOutput) productivities.get(i);
                        if (headcount.getProcessName() == productivity.getProcessName()) {
                            throughputs.add(ThroughputOutput.builder()
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
                input.getDateFrom(), input.getDateTo(), input.getSource(), input.getProcessName());
    }

    private GetEntityInput createHeadcountInput(final GetEntityInput input) {
        return new GetEntityInput(input.getWarehouseId(), input.getWorkflow(), HEADCOUNT,
                input.getDateFrom(), input.getDateTo(), input.getSource(), input.getProcessName());
    }

    private List<EntityOutput> getSimulationThroughput() {
        //TODO: Add SIMULATION logic
        return emptyList();
    }
}
