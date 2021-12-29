package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastIdView;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;

@Service
@AllArgsConstructor
public class GetForecastUseCase {

    private final Repository repository;

    private final RequestScopedMemory requestScopedMemory;

    @Trace
    public List<Long> execute(final GetForecastInput getForecastInput) {
        final var computedForecastsIdsByInput = requestScopedMemory.getComputedForecastsIdsByInput();
        return computedForecastsIdsByInput.computeIfAbsent(getForecastInput, input -> {
            final Set<String> forecastWeeks = getForecastWeeks(input.getDateFrom(), input.getDateTo());
            final List<ForecastIdView> forecastIdViews = repository
                    .findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(
                            input.getWarehouseId(),
                            input.getWorkflow().name(),
                            forecastWeeks
                    );

            if (forecastIdViews.isEmpty()) {
                throw new ForecastNotFoundException(
                        input.getWorkflow().name(), input.getWarehouseId(), forecastWeeks);
            }
            return forecastIdViews.stream()
                    .map(ForecastIdView::getId)
                    .collect(Collectors.toList());
        });
    }

    public interface Repository {
        List<ForecastIdView> findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(
                String warehouseId,
                String workflow,
                Set<String> weeks
        );
    }

    @Getter
    @Service
    @RequestScope
    public static class RequestScopedMemory {
        private final Map<GetForecastInput, List<Long>> computedForecastsIdsByInput = new HashMap<>();
    }
}
