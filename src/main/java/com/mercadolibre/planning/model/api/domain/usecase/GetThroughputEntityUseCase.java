package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetEntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetHeadcountOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetThroughputOutput;
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
    public List<GetEntityOutput> execute(final GetEntityInput input) {
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

    private List<GetEntityOutput> getForecastThroughput(final GetEntityInput input) {
        final List<GetEntityOutput> headcounts = new ArrayList<>(headcountEntityUseCase
                .execute(createHeadcountInput(input)));

        final List<GetEntityOutput> productivities = new ArrayList<>(productivityEntityUseCase
                .execute(createProductivityInput(input)));

        sortByProcessNameAndDate(headcounts, productivities);

        return createThroughputs(headcounts, productivities);
    }

    private List<GetEntityOutput> createThroughputs(final List<GetEntityOutput> headcounts,
                                                    final List<GetEntityOutput> productivities) {

        final List<GetEntityOutput> throughputs = new ArrayList<>();
        if (headcounts.size() == productivities.size()) {
            IntStream.range(0, headcounts.size())
                    .forEach(i -> {
                        final GetHeadcountOutput headcount = (GetHeadcountOutput) headcounts.get(i);
                        final GetProductivityOutput productivity =
                                (GetProductivityOutput) productivities.get(i);
                        if (headcount.getProcessName() == productivity.getProcessName()) {
                            throughputs.add(GetThroughputOutput.builder()
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

    private void sortByProcessNameAndDate(final List<GetEntityOutput> headcounts,
                                          final List<GetEntityOutput> productivities) {
        final Comparator<GetEntityOutput> compareByProcessNameAndDate =
                comparing(GetEntityOutput::getProcessName).thenComparing(GetEntityOutput::getDate);

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

    private List<GetEntityOutput> getSimulationThroughput() {
        //TODO: Add SIMULATION logic
        return emptyList();
    }
}
