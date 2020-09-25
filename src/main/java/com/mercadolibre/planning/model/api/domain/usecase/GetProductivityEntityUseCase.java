package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetEntityOutput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.List;

import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
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
                .findByWarehouseIdAndWorkflowAndProcessNameAndDayTimeInRange(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        input.getProcessName(),
                        getOffsetTime(input.getDateFrom()),
                        getOffsetTime(input.getDateTo()));

        return input.getDateFrom().toLocalDate()
                .datesUntil(input.getDateTo().toLocalDate().plusDays(1))
                .map(day -> productivities.stream()
                        .map(p -> GetEntityOutput.builder()
                                .workflow(input.getWorkflow())
                                .date(getZonedDateTime(p.getDayTime(), day))
                                .processName(p.getProcessName())
                                .value(p.getProductivity())
                                .metricUnit(p.getProductivityMetricUnit())
                                .source(FORECAST)
                                .build())
                        .collect(toList()))
                .flatMap(List::stream)
                .collect(toList());
    }

    private List<GetEntityOutput> getSimulationProductivity() {
        //TODO: Add SIMULATION logic
        return emptyList();
    }

    private OffsetTime getOffsetTime(final TemporalAccessor date) {
        return OffsetTime.from(date);
    }

    private ZonedDateTime getZonedDateTime(final OffsetTime offsetTime, final LocalDate day) {
        return ZonedDateTime.of(day, offsetTime.toLocalTime(), offsetTime.getOffset());
    }
}
