package com.mercadolibre.planning.model.api.client.db.repository.plan;

import com.mercadolibre.planning.model.api.domain.entity.plan.HistoricalDialogSpa;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for CRUD of the historical_dialog_spa table. */
public interface HistoricalDialogSpaRepository extends JpaRepository<HistoricalDialogSpa, Long> {

}
