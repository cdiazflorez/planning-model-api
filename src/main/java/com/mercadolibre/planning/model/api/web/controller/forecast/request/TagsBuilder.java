package com.mercadolibre.planning.model.api.web.controller.forecast.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;

/**
 * Interface for generate tags for staffing plan.
 */

public interface TagsBuilder {

  ProcessPath getProcessPath();

  ProcessName getProcessName();

  String getHeadcountType();

  int getAbilityLevel();

  ProcessingType getType();
}
