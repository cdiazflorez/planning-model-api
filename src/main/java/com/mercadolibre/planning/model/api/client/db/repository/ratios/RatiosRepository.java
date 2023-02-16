package com.mercadolibre.planning.model.api.client.db.repository.ratios;

import com.mercadolibre.planning.model.api.domain.entity.ratios.Ratio;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Repository for CRUD of the ratios table. */
public interface RatiosRepository extends JpaRepository<Ratio, Long> {

    @Transactional
    @Modifying
    @Query("delete from ratios u where u.date <= :date and u.type = :type")
    void deleteRatios(@Param("date") Instant date, @Param("type") String type);

    List<Ratio> findRatiosByLogisticCenterIdAndDateBetweenAndType(
        String logisticCenterId,
        Instant dateFrom,
        Instant dateTo,
        String type
    );
}
