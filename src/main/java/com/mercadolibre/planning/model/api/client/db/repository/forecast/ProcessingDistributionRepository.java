package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface ProcessingDistributionRepository
        extends CrudRepository<ProcessingDistribution, Long> {

    @Query(value = "SELECT p.date, process_name as processName, quantity,"
            + " quantity_metric_unit as quantityMetricUnit, type "
            + "FROM processing_distribution p "
            + "WHERE p.process_name IN (:process_name) "
            + "AND p.date BETWEEN :date_from AND :date_to "
            + "AND (COALESCE(:type) is NULL OR p.type IN (:type)) "
            + "AND p.forecast_id in (:forecast_ids)", nativeQuery = true)
    List<ProcessingDistributionView> findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            @Param("type") Set<String> type,
            @Param("process_name") List<String> processNames,
            @Param("date_from") ZonedDateTime dateFrom,
            @Param("date_to") ZonedDateTime dateTo,
            @Param("forecast_ids") List<Long> forecastIds);
}
