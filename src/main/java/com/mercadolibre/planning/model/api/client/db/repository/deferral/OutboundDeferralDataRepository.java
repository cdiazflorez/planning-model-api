package com.mercadolibre.planning.model.api.client.db.repository.deferral;

import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for CRUD of the deferral_data and deferral_sla_status tables.
 */
@Repository
public interface OutboundDeferralDataRepository extends JpaRepository<OutboundDeferralData, Long> {

  /**
   * Finds a list of DeferralDto objects with the specified logistic center ID and dates within the given range.
   *
   * @param lcId     The logistic center ID to search for.
   * @param dateFrom The start date of the range (inclusive).
   * @param dateTo   The end date of the range (inclusive).
   * @return A list of DeferralDto objects matching the criteria.
   */
  List<OutboundDeferralData> findByLogisticCenterIdAndDateBetweenAndUpdatedIsTrue(String lcId, Instant dateFrom, Instant dateTo);

  @Modifying
  @Transactional
  @Query("DELETE FROM OutboundDeferralData odd WHERE odd.date < :date")
  int deleteByDateBefore(Instant date);

  @Query(
      "FROM OutboundDeferralData "
          + "WHERE logisticCenterId = :logisticCenterId "
          + "AND date IN "
          + " (SELECT MAX(odd.date) FROM OutboundDeferralData odd "
          + "   WHERE odd.logisticCenterId = :logisticCenterId)"
  )
  List<OutboundDeferralData> getLastCptDeferralReportForLogisticCenter(String logisticCenterId);

}
