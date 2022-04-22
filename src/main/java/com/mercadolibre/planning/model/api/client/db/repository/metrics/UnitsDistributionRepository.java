package com.mercadolibre.planning.model.api.client.db.repository.metrics;

import com.mercadolibre.planning.model.api.domain.entity.metrics.UnitsDistribution;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitsDistributionRepository extends JpaRepository<UnitsDistribution, Long> {

  List<UnitsDistribution> findByDateBetweenAndLogisticCenterId(ZonedDateTime zonedDateTimeStar, ZonedDateTime zonedDateTimeEnd,
                                                               String logisticCenterId);
}
