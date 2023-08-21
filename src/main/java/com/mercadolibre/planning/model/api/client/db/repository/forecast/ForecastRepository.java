package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForecastRepository extends GetForecastUseCase.Repository, CrudRepository<Forecast, Long> {

  @Override
  @Query(value = "SELECT MAX(f.id) as id"
      + "     FROM forecast f "
      + "     WHERE f.workflow = :workflow "
      + "     AND f.logistic_center_id = :warehouse_id "
      + "     AND f.week in (:weeks)"
      + "     AND f.date_created <= :view_date"
      + "     GROUP BY f.week",
      nativeQuery = true)
  List<ForecastIdView> findForecastIdsByWarehouseIdAAndWorkflowAndWeeksAtViewDate(
      @Param("warehouse_id") String warehouseId,
      @Param("workflow") String workflow,
      @Param("weeks") Set<String> weeks,
      @Param("view_date") Instant viewDate
  );
}
