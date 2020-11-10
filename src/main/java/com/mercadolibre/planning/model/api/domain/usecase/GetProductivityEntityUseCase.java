package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class GetProductivityEntityUseCase implements GetEntityUseCase {

    private final HeadcountProductivityRepository productivityRepository;
    private final CurrentHeadcountProductivityRepository currentProductivityRepository;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        if (input.getSource() == FORECAST) {
            return getForecastProductivity(input);
        } else {
            return getSimulationProductivity(input);
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

    private List<EntityOutput> getSimulationProductivity(final GetEntityInput input) {
        final Workflow workflow = input.getWorkflow();
        final List<EntityOutput> forecastProductivities = getForecastProductivity(input);
        final List<CurrentHeadcountProductivity> productivities = currentProductivityRepository
                .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        input.getProcessName(),
                        input.getDateFrom(),
                        input.getDateTo()
                );
        final List<EntityOutput> entities = new ArrayList<>();

        forecastProductivities.forEach(fp ->
                entities.add(EntityOutput.builder()
                                .workflow(workflow)
                                .value(fp.getValue())
                                .source(FORECAST)
                                .processName(fp.getProcessName())
                                .metricUnit(fp.getMetricUnit())
                                .date(fp.getDate())
                                .build()));

        productivities.forEach(sp ->
                entities.add(EntityOutput.builder()
                        .workflow(workflow)
                        .value(sp.getProductivity())
                        .source(SIMULATION)
                        .processName(sp.getProcessName())
                        .metricUnit(sp.getProductivityMetricUnit())
                        .date(sp.getDate())
                        .build()));

        return entities;
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
