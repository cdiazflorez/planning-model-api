package com.mercadolibre.planning.model.api.client.db.repository.configuration;

import com.mercadolibre.planning.model.api.domain.entity.configuration.OutboundProcessingTime;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for CRUD of the outbound_processing_time_repository table.
 */
public interface OutboundProcessingTimeRepository extends JpaRepository<OutboundProcessingTime, Long> {

  @Modifying
  @Query(
      "UPDATE OutboundProcessingTime opt "
          + "SET opt.isActive = false "
          + "WHERE opt.logisticCenterID = :logistic_center_id "
          + "AND opt.etdDay = :etd_day "
          + "AND opt.etdHour = :etd_hour "
          + "AND opt.isActive = true"
  )
  void deactivateByLogisticCenterAndCpt(
      @Param("logistic_center_id") String logisticCenterID,
      @Param("etd_day") String etdDay,
      @Param("etd_hour") String etdHour
  );

  @Modifying
  @Query(
      "UPDATE OutboundProcessingTime opt "
          + "SET opt.isActive = false "
          + "WHERE opt.logisticCenterID = :logistic_center_id "
          + "AND opt.isActive = true"
  )
  void deactivateAllByLogisticCenter(
      @Param("logistic_center_id") String logisticCenterID
  );

  @Modifying
  @Query(
      "DELETE FROM OutboundProcessingTime opt "
          + "WHERE opt.dateCreated < :beforeAt "
          + "AND opt.isActive = false"
  )
  void purgeOldRecords(Instant beforeAt);

  @Query(
      "SELECT opt "
          + "FROM OutboundProcessingTime opt "
          + "WHERE opt.logisticCenterID = :logistic_center_id "
          + "AND opt.isActive = true"
  )
  List<OutboundProcessingTime> findByLogisticCenterAndIsActive(
      @Param("logistic_center_id") String logisticCenterID
  );
}
