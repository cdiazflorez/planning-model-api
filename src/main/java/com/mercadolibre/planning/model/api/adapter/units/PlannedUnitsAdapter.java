package com.mercadolibre.planning.model.api.adapter.units;

import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.DATE_IN;
import static com.mercadolibre.planning.model.api.util.DateUtils.max;
import static com.mercadolibre.planning.model.api.util.DateUtils.min;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionDynamicRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService.PlannedUnitsGateway;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlannedUnitsAdapter implements PlannedUnitsGateway {

  private final GetForecastUseCase getForecastUseCase;

  private final PlanningDistributionDynamicRepository repository;

  private static List<PlanningDistribution> resolverForecastOverlapping(final List<PlanningDistribution> distributions) {
    final var maxForecastIdByDateIn = distributions.stream()
        .collect(Collectors.toMap(
            PlanningDistribution::getDateIn,
            PlanningDistribution::getForecastId,
            Long::max
        ));

    return distributions.stream()
        .filter(d -> maxForecastIdByDateIn.get(d.getDateIn()).equals(d.getForecastId()))
        .collect(toList());
  }

  public List<PlanningDistribution> getPlanningDistributions(
      final String logisticCenterId,
      final Workflow workflow,
      final Set<ProcessPath> processPaths,
      final Instant dateInFrom,
      final Instant dateInTo,
      final Instant dateOutFrom,
      final Instant dateOutTo,
      final Set<Grouper> groupBy,
      final Instant viewDate
  ) {
    final var dateFrom = min(dateInFrom, dateOutFrom).orElseThrow();
    final var dateTo = max(dateInTo, dateOutTo).orElseThrow();

    final List<Long> forecastIds = getForecastUseCase.execute(new GetForecastInput(
        logisticCenterId,
        workflow,
        dateFrom,
        dateTo,
        viewDate
    ));

    final var distributions = repository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        dateInFrom,
        dateInTo,
        dateOutFrom,
        dateOutTo,
        processPaths,
        groupBy,
        new HashSet<>(forecastIds)
    );

    return groupBy.contains(DATE_IN) ? resolverForecastOverlapping(distributions) : distributions;
  }
}
