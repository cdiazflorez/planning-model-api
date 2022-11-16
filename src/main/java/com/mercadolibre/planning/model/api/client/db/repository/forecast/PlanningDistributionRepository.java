package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionElemView;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface PlanningDistributionRepository extends CrudRepository<PlanningDistribution, Long> {

  @Query(value = "SELECT p.forecast_id as forecastId, date_in as dateIn, date_out as dateOut,"
      + "     SUM(quantity) as quantity "
      + "FROM planning_distribution p "
      + "WHERE (:date_out_from IS NULL OR :date_out_from <= p.date_out) "
      + "   AND (:date_out_to IS NULL OR p.date_out <= :date_out_to) "
      + "   AND (:date_in_from IS NULL OR :date_in_from <= p.date_in) "
      + "   AND (:date_in_to IS NULL OR p.date_in <= :date_in_to) "
      + "   AND p.forecast_id in (:forecast_ids) "
      + "GROUP BY p.forecast_id, date_in, date_out "
      + "ORDER BY p.forecast_id DESC",
      nativeQuery = true)
  List<PlanningDistributionElemView> findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
      @Param("date_out_from") ZonedDateTime dateOutFrom,
      @Param("date_out_to") ZonedDateTime dateOutTo,
      @Param("date_in_from") ZonedDateTime dateInFrom,
      @Param("date_in_to") ZonedDateTime dateInTo,
      @Param("forecast_ids") List<Long> forecastIds);
}
