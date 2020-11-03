package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class GetProductivityEntityUseCase implements GetEntityUseCase {

    private final HeadcountProductivityRepository productivityRepository;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        if (input.getSource() == null || input.getSource() == FORECAST) {
            return getForecastProductivity(input);
        } else {
            return getSimulationProductivity();
        }
    }

    private List<EntityOutput> getForecastProductivity(final GetEntityInput input) {
        final List<HeadcountProductivityView> productivities = productivityRepository
                .findByWarehouseIdAndWorkflowAndProcessName(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        input.getProcessName().stream().map(Enum::name).collect(toList()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        getForecastWeeks(input.getDateFrom(), input.getDateTo()));

        return createEntityOutputs(productivities, input.getWorkflow());
    }

    private List<EntityOutput> getSimulationProductivity() {
        //TODO: Add SIMULATION logic
        return emptyList();
    }

    private List<EntityOutput> createEntityOutputs(
            final List<HeadcountProductivityView> productivities,
            final Workflow workflow) {

        return productivities.stream()
                .map(p -> EntityOutput.builder()
                        .workflow(workflow)
                        .date(ofInstant(p.getDate().toInstant(), UTC))
                        .processName(p.getProcessName())
                        .value(p.getProductivity())
                        .metricUnit(p.getProductivityMetricUnit())
                        .source(FORECAST)
                        .build())
                .collect(toList());
    }

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == PRODUCTIVITY;
    }
}
