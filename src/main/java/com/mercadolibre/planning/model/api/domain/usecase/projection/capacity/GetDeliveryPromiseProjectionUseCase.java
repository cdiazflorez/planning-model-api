package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.DeliveryPromiseProjectionUtils.getSlasToBeProjectedFromBacklogAndKnowSlas;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;

import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityService;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
@Slf4j
@Component
@AllArgsConstructor
public class GetDeliveryPromiseProjectionUseCase {

  private final MaxCapacityService maxCapacityService;

  private final GetCycleTimeService getCycleTimeService;

  private final GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  public List<DeliveryPromiseProjectionOutput> execute(final GetDeliveryPromiseProjectionInput input) {

    final List<GetSlaByWarehouseOutput> allCptByWarehouse = getSlaByWarehouseOutboundService.execute(
        new GetSlaByWarehouseInput(
            input.getWarehouseId(),
            input.getDateFrom(),
            input.getDateTo(),
            DeliveryPromiseProjectionUtils.getCptDefaultFromBacklog(input.getBacklog()),
            input.getTimeZone()
        ));

    final List<ZonedDateTime> slas = getSlasToBeProjectedFromBacklogAndKnowSlas(input.getBacklog(), allCptByWarehouse);

    final Map<ZonedDateTime, Long> cycleTimeByCpt = getCycleTimeService.execute(new GetCycleTimeInput(input.getWarehouseId(), slas));

    return DeliveryPromiseCalculator.calculate(
        input.getDateFrom(),
        input.getDateTo(),
        getCurrentUtcDate(),
        input.getBacklog(),
        getMaxCapacity(input),
        allCptByWarehouse,
        cycleTimeByCpt
    );
  }

  private Map<ZonedDateTime, Integer> getMaxCapacity(final GetDeliveryPromiseProjectionInput input) {

    return maxCapacityService.getMaxCapacity(new MaxCapacityInput(
        input.getWarehouseId(),
        input.getWorkflow(),
        input.getDateFrom(),
        input.getDateTo(),
        input.getSimulations()));
  }

}
