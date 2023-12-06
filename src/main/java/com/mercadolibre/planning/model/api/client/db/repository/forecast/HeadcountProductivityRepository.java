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

    @Query(value = "SELECT process_name as processName, productivity, process_path as processPath, hd.date as date, "
            + "productivity_metric_unit as productivityMetricUnit, ability_level as abilityLevel "
            + "FROM headcount_productivity hd "
            + "WHERE hd.forecast_id in (:forecast_ids)"
            + "AND (COALESCE(:process_path) IS NULL OR hd.process_path IN (:process_path))"
            + "AND hd.date BETWEEN :date_from AND :date_to "
            + "AND hd.ability_level in (:ability_levels)  "
            + "AND (COALESCE(:process_name) IS NULL OR hd.process_name IN (:process_name)) ", nativeQuery = true)
    List<HeadcountProductivityView> findBy(
            @Param("process_name") List<String> processNames,
            @Param("process_path") List<String> processPaths,
            @Param("date_from") ZonedDateTime dateFrom,
            @Param("date_to") ZonedDateTime dateTo,
            @Param("forecast_ids") List<Long> forecastIds,
            @Param("ability_levels") Set<Integer> abilityLevels
    );
}
