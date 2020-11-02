package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HeadcountProductivityRepository
        extends CrudRepository<HeadcountProductivity, Long> {

    @Query("SELECT "
            + " hd "
            + "FROM HeadcountProductivity hd "
            + "WHERE "
            + "   hd.processName IN (:process_name) "
            + "   AND hd.abilityLevel = 1  "
            + "   AND hd.forecast.id =  "
            + "   ("
            + "      SELECT "
            + "         MAX(f.id)  "
            + "      FROM Forecast f, ForecastMetadata fm  "
            + "      WHERE "
            + "         f.workflow = :workflow "
            + "         AND fm.forecastId = f.id  "
            + "         AND fm.key = 'warehouse_id'  "
            + "         AND fm.value = :warehouse_id"
            + "   ) "
            + "ORDER BY hd.dayTime")
    List<HeadcountProductivity> findByWarehouseIdAndWorkflowAndProcessName(
            @Param("warehouse_id") String warehouseId,
            @Param("workflow") Workflow workflow,
            @Param("process_name") List<ProcessName> processNames
    );
}
