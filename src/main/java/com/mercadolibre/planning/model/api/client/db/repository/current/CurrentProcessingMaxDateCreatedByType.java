package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import java.time.ZonedDateTime;

/**
 * Interface to get max date created of current processing distribution.
 */
public interface CurrentProcessingMaxDateCreatedByType {

  ProcessingType getType();

  ZonedDateTime getDateCreated();

}
