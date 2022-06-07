package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.exception.InvalidForecastException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
public class GetDeliveryPromiseProjectionUseCase extends GetProjectionUseCase {


    public GetDeliveryPromiseProjectionUseCase(
        CalculateCptProjectionUseCase calculatedProjectionUseCase,
        ProcessingDistributionRepository processingDistRepository,
        GetForecastUseCase getForecastUseCase,
        GetCycleTimeService getCycleTimeService,
        GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService,
        PlannedBacklogService plannedBacklogService) {
        super(calculatedProjectionUseCase, processingDistRepository, getForecastUseCase, getCycleTimeService,
            getSlaByWarehouseOutboundService,
            plannedBacklogService);
    }

    protected Map<ZonedDateTime, Integer> getMaxCapacity(final GetDeliveryPromiseProjectionInput input) {

        final List<ProcessingDistributionView> processingDistributionView = processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        Set.of(MAX_CAPACITY.name()),
                        List.of(GLOBAL.toJson()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        getForecastIds(input));

        final Map<Instant, Integer> capacityByDate =
                processingDistributionView.stream()
                        .collect(
                                toMap(
                                        o -> o.getDate().toInstant().truncatedTo(SECONDS),
                                        o -> (int) o.getQuantity(),
                                        (intA, intB) -> intB));

        final int defaultCapacity = capacityByDate.values().stream()
                .max(Integer::compareTo)
                .orElseThrow(() ->
                        new InvalidForecastException(
                                input.getWarehouseId(),
                                input.getWorkflow().name())
                );

        final Set<Instant> capacityHours = getCapacityHours(input.getDateFrom(), input.getDateTo());

        return capacityHours.stream()
                .collect(
                        toMap(
                                o -> ZonedDateTime.from(o.atZone(ZoneOffset.UTC)),
                                o -> capacityByDate.getOrDefault(o, defaultCapacity),
                                (intA, intB) -> intB,
                                TreeMap::new));
    }

}
