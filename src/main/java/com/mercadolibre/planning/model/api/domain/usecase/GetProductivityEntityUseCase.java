package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetEntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.ProductivityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;

import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.iterate;

@AllArgsConstructor
@Service
public class GetProductivityEntityUseCase implements GetEntityUseCase {

    private final HeadcountProductivityRepository productivityRepository;

    @Override
    public List<GetEntityOutput> execute(final GetEntityInput input) {
        if (input.getSource() == null || input.getSource() == FORECAST) {
            return getForecastProductivity(input);
        } else {
            return getSimulationProductivity();
        }
    }

    private List<GetEntityOutput> getForecastProductivity(final GetEntityInput input) {
        final List<HeadcountProductivity> productivities = productivityRepository
                .findByWarehouseIdAndWorkflowAndProcessName(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        input.getProcessName());

        final ZonedDateTime dateFrom = input.getDateFrom();
        final List<GetEntityOutput> result = new ArrayList<>();
        iterate(dateFrom, date -> date.plusHours(1))
                .limit(ChronoUnit.HOURS.between(dateFrom, input.getDateTo()) + 1)
                .forEach(dateTime -> result.addAll(
                        createEntityOutputs(productivities, dateTime, input.getWorkflow())));

        return result;
    }

    private List<GetEntityOutput> getSimulationProductivity() {
        //TODO: Add SIMULATION logic
        return emptyList();
    }

    private List<GetEntityOutput> createEntityOutputs(
            final List<HeadcountProductivity> productivities,
            final ZonedDateTime dateTime,
            final Workflow workflow) {

        return productivities.stream()
                .filter(l -> areTheSameDayTimes(dateTime, l.getDayTime()))
                .map(p -> ProductivityOutput.builder()
                        .workflow(workflow)
                        .date(dateTime)
                        .processName(p.getProcessName())
                        .value(p.getProductivity())
                        .metricUnit(p.getProductivityMetricUnit())
                        .source(FORECAST)
                        .build())
                .collect(toList());
    }

    private boolean areTheSameDayTimes(final TemporalAccessor dateTime,
                                       final OffsetTime productivityDayTime) {
        return productivityDayTime.toString().equals(OffsetTime.from(dateTime).toString());
    }

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == PRODUCTIVITY;
    }
}
