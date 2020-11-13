package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface HeadcountProductivityRepository
        extends CrudRepository<HeadcountProductivity, Long> {

    @Query(value = "SELECT process_name as processName, productivity, hd.date as date, "
            + "productivity_metric_unit as productivityMetricUnit "
            + "FROM headcount_productivity hd "
            + "WHERE hd.process_name IN (:process_name) "
            + "AND hd.date BETWEEN :date_from AND :date_to "
            + "AND hd.ability_level in (:ability_levels)  "
            + "AND hd.forecast_id in ("
            + " SELECT MAX(id) FROM "
            + "     (SELECT id, "
            + "     workflow, "
            + "     (SELECT m.value FROM forecast_metadata m "
            + "     WHERE m.forecast_id = f.id AND m.key = 'warehouse_id') AS warehouse_id, "
            + "     (SELECT m.value FROM forecast_metadata m "
            + "     WHERE m.forecast_id = f.id AND m.key = 'week') AS forecast_week "
            + "     FROM forecast f) forecast_with_metadata "
            + "     WHERE warehouse_id = :warehouse_id "
            + "     AND workflow = :workflow "
            + "     AND forecast_week IN (:weeks) "
            + "     GROUP BY forecast_week)", nativeQuery = true)
    List<HeadcountProductivityView> findBy(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") String workflow,
            @Param("process_name") List<String> processNames,
            @Param("date_from") ZonedDateTime dateFrom,
            @Param("date_to") ZonedDateTime dateTo,
            @Param("weeks") Set<String> weeks,
            @Param("ability_levels") Set<Integer> abilityLevels
    );
}
