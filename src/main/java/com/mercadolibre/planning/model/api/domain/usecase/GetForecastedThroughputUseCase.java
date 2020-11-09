package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;

@Service
@AllArgsConstructor
public class GetForecastedThroughputUseCase extends GetThroughputEntityUseCase {

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

        return createThroughputs(headcounts, productivities);
    }

    private GetEntityInput createProductivityInput(final GetEntityInput input) {
        return new GetEntityInput(input.getWarehouseId(), input.getWorkflow(), PRODUCTIVITY,
                input.getDateFrom(), input.getDateTo(), input.getSource(),
                input.getProcessName(), input.getProcessingType(), null);
    }

    private GetEntityInput createHeadcountInput(final GetEntityInput input) {
        return new GetEntityInput(input.getWarehouseId(), input.getWorkflow(), HEADCOUNT,
                input.getDateFrom(), input.getDateTo(), input.getSource(),
                input.getProcessName(), THROUGHPUT_PROCESSING_TYPES, null);
    }
}
