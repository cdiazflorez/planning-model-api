package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ForecastRepository extends CrudRepository<Forecast, Long> {

    @Query(value = "SELECT MAX(fm.id) FROM "
            + "     (SELECT id, "
            + "     workflow, "
            + "     (SELECT m.value FROM forecast_metadata m "
            + "     WHERE m.forecast_id = f.id AND m.key = 'warehouse_id') AS warehouse_id, "
            + "     (SELECT m.value FROM forecast_metadata m "
            + "     WHERE m.forecast_id = f.id AND m.key = 'week') AS forecast_week "
            + "     FROM forecast f) fm "
            + "     WHERE fm.warehouse_id = :warehouse_id "
            + "     AND fm.workflow = :workflow "
            + "     AND fm.forecast_week in (:weeks)"
            + "     GROUP BY fm.forecast_week", nativeQuery = true)
    List<Long> findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") String workflow,
            @Param("weeks") Set<String> weeks
    );
}
