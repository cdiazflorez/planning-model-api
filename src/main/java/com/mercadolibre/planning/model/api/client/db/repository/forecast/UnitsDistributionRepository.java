package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.UnitsDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;

public interface UnitsDistributionRepository extends JpaRepository<UnitsDistribution, Long> {

    List<UnitsDistribution> findByDateBetweenaAndLogisticCenterId(ZonedDateTime zonedDateTimeStar,ZonedDateTime zonedDateTimeEnd,String logisticCenterId);
}
