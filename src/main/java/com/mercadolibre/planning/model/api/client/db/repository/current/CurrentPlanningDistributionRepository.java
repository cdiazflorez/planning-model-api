package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * CurrentPlanningDistributionRepository get backlog planed from forecast between dates of capacity used.
 */
public interface CurrentPlanningDistributionRepository extends CrudRepository<CurrentPlanningDistribution, Long> {

    List<CurrentPlanningDistribution> findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(Workflow workflow,
                                                                                                        String warehouseId,
                                                                                                        ZonedDateTime dateOutFrom,
                                                                                                        ZonedDateTime dateOutTo
    );
}
