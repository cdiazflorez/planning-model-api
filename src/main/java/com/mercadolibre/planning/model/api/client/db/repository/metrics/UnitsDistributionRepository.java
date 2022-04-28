package com.mercadolibre.planning.model.api.client.db.repository.metrics;

import com.mercadolibre.planning.model.api.domain.entity.metrics.UnitsDistribution;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for CRUD of the Units-distribution table. */
public interface UnitsDistributionRepository extends JpaRepository<UnitsDistribution, Long> {

  /**
   * @param zonedDateTimeStar Search start date.
   * @param zonedDateTimeEnd  Search end date.
   * @param logisticCenterId  ID of the warehouse.
   * @return Returns a list of the Units-distribution.
   */
  List<UnitsDistribution> findByDateBetweenAndLogisticCenterId(ZonedDateTime zonedDateTimeStar, ZonedDateTime zonedDateTimeEnd,
                                                               String logisticCenterId);
}
