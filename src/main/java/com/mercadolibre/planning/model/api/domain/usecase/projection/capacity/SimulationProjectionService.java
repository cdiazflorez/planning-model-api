package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
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
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SimulationProjectionService extends GetProjectionUseCase {

  public SimulationProjectionService(
      CalculateCptProjectionUseCase calculatedProjectionUseCase,
      ProcessingDistributionRepository processingDistRepository,
      GetForecastUseCase getForecastUseCase,
      GetCycleTimeService getCycleTimeService,
      GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService,
      PlannedBacklogService plannedBacklogService) {
    super(calculatedProjectionUseCase, processingDistRepository, getForecastUseCase, getCycleTimeService, getSlaByWarehouseOutboundService,
        plannedBacklogService);
  }

  @Override
  protected Map<ZonedDateTime, Integer> getMaxCapacity(GetDeliveryPromiseProjectionInput input) {

    final List<ProcessingDistributionView> maximumCapacity = processingDistRepository
        .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            Set.of(MAX_CAPACITY.name()),
            List.of(GLOBAL.toJson()),
            input.getDateFrom(),
            input.getDateTo(),
            getForecastIds(input));

    final Map<ZonedDateTime, Integer> capacity =
        maximumCapacity.stream()
            .collect(
                toMap(
                    o -> o.getDate().toInstant().truncatedTo(SECONDS).atZone(ZoneOffset.UTC),
                    o -> (int) o.getQuantity(),
                    (intA, intB) -> intB));

    if (input.getSimulations() != null && !input.getSimulations().isEmpty()) {
      Map<ZonedDateTime, Integer> simulationDates = input.getSimulations().stream()
          .filter(simulation -> simulation.getProcessName().equals(GLOBAL))
          .map(Simulation::getEntities)
          .flatMap(Collection::stream)
          .filter(simulationEntity -> simulationEntity.getType().equals(THROUGHPUT))
          .map(SimulationEntity::getValues)
          .flatMap(Collection::stream)
          .collect(toMap(quantityByDate -> quantityByDate.getDate().withZoneSameInstant(ZoneOffset.UTC), QuantityByDate::getQuantity));

      capacity.putAll(simulationDates);

    }

    return capacity;
  }

}
