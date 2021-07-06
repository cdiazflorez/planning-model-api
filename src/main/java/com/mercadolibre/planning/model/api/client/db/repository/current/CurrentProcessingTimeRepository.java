package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CurrentProcessingTimeRepository extends
        JpaRepository<CurrentProcessingTime, Long> {

    @Query("SELECT "
            + " cpt "
            + "FROM CurrentProcessingTime cpt "
            + "WHERE "
            + "   cpt.workflow = :workflow "
            + "   AND cpt.logisticCenterId = :warehouse_id "
            + "ORDER BY cpt.id desc")
    List<CurrentProcessingTime>
            findByWorkflowAndLogisticCenterIdAndIsActiveTrueAndDateBetweenCpt(
                    @Param("workflow") Workflow workflow,
                    @Param("warehouse_id") String warehouseId
    );
}
