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
  @Query(value = "SELECT MAX(fm.id) as id FROM "
      + "     (SELECT id, workflow, date_created, "
      + "     (SELECT m.`value` FROM forecast_metadata m "
      + "     WHERE m.forecast_id = f.id AND m.`key` = 'warehouse_id') AS warehouse_id, "
      + "     (SELECT m.`value` FROM forecast_metadata m "
      + "     WHERE m.forecast_id = f.id AND m.`key` = 'week') AS forecast_week "
      + "     FROM forecast f) fm "
      + "     WHERE fm.warehouse_id = :warehouse_id "
      + "     AND fm.workflow = :workflow "
      + "     AND fm.forecast_week in (:weeks)"
      + "     AND fm.date_created <= :view_date"
      + "     GROUP BY fm.forecast_week",
      nativeQuery = true)
  List<ForecastIdView> findForecastIdsByWarehouseIdAAndWorkflowAndWeeksAtViewDate(
      @Param("warehouse_id") String warehouseId,
      @Param("workflow") String workflow,
      @Param("weeks") Set<String> weeks,
      @Param("view_date") Instant viewDate
  );
}
