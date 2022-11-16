package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Planning distribution repository.
 * Obtain dynamic planning distribution
 * */
public interface PlanningDistributionRepository {
  List<PlanningDistribution> findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
      Instant dateInFrom,
      Instant dateInTo,
      Instant dateOutFrom,
      Instant dateOutTo,
      Set<ProcessPath> processPaths,
      Set<Grouper> groupers,
      Set<Long> forecastIds
  );
}
