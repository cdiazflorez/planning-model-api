package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Planning distribution repository.
 * Obtain dynamic planning distribution
 * */
public interface PlanningDistributionGateway {
  List<PlanDistribution> findByForecastIdsAndDynamicFilters(
      Instant dateInFrom,
      Instant dateInTo,
      Instant dateOutFrom,
      Instant dateOutTo,
      Set<ProcessPath> processPaths,
      Set<Long> forecastIds
  );
}
