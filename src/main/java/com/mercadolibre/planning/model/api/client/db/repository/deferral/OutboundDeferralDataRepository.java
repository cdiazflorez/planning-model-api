package com.mercadolibre.planning.model.api.client.db.repository.deferral;

import com.mercadolibre.planning.model.api.domain.entity.deferral.OutboundDeferralData;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for CRUD of the deferral_data and deferral_sla_status tables.
 */
public interface OutboundDeferralDataRepository extends JpaRepository<OutboundDeferralData, Long> {

}
