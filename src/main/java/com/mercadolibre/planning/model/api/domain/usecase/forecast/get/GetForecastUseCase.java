package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastIdView;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastRepository;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;

@Service
@AllArgsConstructor
public class GetForecastUseCase implements UseCase<GetForecastInput, List<ForecastIdView>> {

    private final ForecastRepository forecastRepository;

    @Trace
    @Override
    public List<ForecastIdView> execute(final GetForecastInput input) {
        final Set<String> forecastWeeks = getForecastWeeks(input.getDateFrom(), input.getDateTo());
        final List<ForecastIdView> forecastIds = forecastRepository
                .findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        forecastWeeks
                );

        if (forecastIds.isEmpty()) {
            throw new ForecastNotFoundException(
                    input.getWorkflow().name(), input.getWarehouseId(), forecastWeeks);
        }

        return forecastIds;
    }
}
