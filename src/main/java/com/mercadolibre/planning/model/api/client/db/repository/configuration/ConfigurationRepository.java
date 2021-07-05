package com.mercadolibre.planning.model.api.client.db.repository.configuration;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConfigurationRepository extends JpaRepository<Configuration, ConfigurationId> {

    @Query("SELECT "
            + " cfg "
            + "FROM Configuration cfg "
            + "WHERE "
            + "   cfg.logisticCenterId = :logistic_center_id ")
    List<Configuration> findByWarehouseId(
            @Param("logistic_center_id") String logisticCenterId
    );
}
