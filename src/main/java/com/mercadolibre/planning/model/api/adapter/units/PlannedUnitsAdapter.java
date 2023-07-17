package com.mercadolibre.planning.model.api.adapter.units;

import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.DATE_IN;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.DATE_OUT;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.PROCESS_PATH;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlannedUnitsAdapter implements PlannedUnitsGateway {

  private static final Map<Grouper, Function<PlanningDistribution, ?>> GROUPER_TO_FUNCTION = Map.of(
      DATE_IN, PlanningDistribution::getDateIn,
      DATE_OUT, PlanningDistribution::getDateOut,
      PROCESS_PATH, PlanningDistribution::getProcessPath
  );

  private final GetForecastUseCase getForecastUseCase;

  private final PlanningDistributionDynamicRepository repository;

  private static List<PlanningDistribution> resolverForecastOverlapping(final List<PlanningDistribution> distributions,
                                                                        final Set<Grouper> groupers) {

    final Grouper grouper = groupers.stream()
        .min(Comparator.comparingInt(Grouper::getPriorityOrderToGroup))
        .orElseThrow();

    final var maxForecastIdByGrouper = distributions.stream()
        .collect(Collectors.toMap(
            GROUPER_TO_FUNCTION.get(grouper),
            PlanningDistribution::getForecastId,
            Long::max
        ));

    return distributions.stream()
        .filter(d -> maxForecastIdByGrouper.get(GROUPER_TO_FUNCTION.get(grouper).apply(d)).equals(d.getForecastId()))
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

    return resolverForecastOverlapping(distributions, groupBy);
  }
}
