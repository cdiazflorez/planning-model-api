package com.mercadolibre.planning.model.api.client.db.repository.deferral;

import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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

    /**
     * Retrieves a list of CPTs that were deferred before the specified {@code viewDate}
     * for the given {@code warehouseId}.
     *
     * @param warehouseId The ID of the logistic center to search for.
     * @param viewDate    The date before which the CPTs were deferred (exclusive).
     * @param deferralType    The status provide which the CPTs were deferred.
     * @return A list of CPTs that were deferred before the specified date.
     */
    @Query("SELECT dd.cpt "
            + "FROM OutboundDeferralData dd "
            + "WHERE dd.logisticCenterId = :warehouseId "
            + "AND dd.date IN ("
            + "  SELECT MAX(id.date) "
            + "  FROM OutboundDeferralData id "
            + "  WHERE id.logisticCenterId = :warehouseId "
            + "  AND id.date <= :viewDate"
            + ") AND dd.status in :deferralType"
    )
    List<Instant> findDeferredCpts(String warehouseId, Instant viewDate, Set<DeferralType> deferralType);

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
