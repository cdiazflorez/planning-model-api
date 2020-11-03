package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface CurrentProcessingDistributionRepository
        extends CrudRepository<CurrentProcessingDistribution, Long> {

    @Query("SELECT "
            + " cpd "
            + "FROM CurrentProcessingDistribution cpd "
            + "WHERE "
            + "   cpd.processName IN (:process_name) "
            + "   AND cpd.date BETWEEN :date_from AND :date_to "
            + "   AND cpd.workflow = :workflow "
            + "   AND cpd.type = :type "
            + "   AND cpd.logisticCenterId = :warehouse_id "
            + "   AND cpd.isActive is true "
            + "ORDER BY cpd.date")
    List<CurrentProcessingDistribution>
            findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") Workflow workflow,
            @Param("type") ProcessingType type,
            @Param("process_name") List<ProcessName> processNames,
            @Param("date_from") ZonedDateTime dateFrom,
            @Param("date_to") ZonedDateTime dateTo);

}
