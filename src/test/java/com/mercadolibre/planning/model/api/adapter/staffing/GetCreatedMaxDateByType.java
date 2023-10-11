package com.mercadolibre.planning.model.api.adapter.staffing;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingMaxDateCreatedByType;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GetCreatedMaxDateByType implements CurrentProcessingMaxDateCreatedByType {

  private ProcessingType type;
  private ZonedDateTime dateCreated;

  @Override
  public ProcessingType getType() {
    return this.type;
  }

  @Override
  public ZonedDateTime getDateCreated() {
    return this.dateCreated;
  }
}
