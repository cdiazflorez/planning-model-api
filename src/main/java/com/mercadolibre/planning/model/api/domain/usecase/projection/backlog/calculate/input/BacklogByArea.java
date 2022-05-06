package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input;

import static com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.Backlog;

import java.util.List;
import lombok.Value;

/**
 * Backlog for each area for a specific instant and process.
 */
@Value
public class BacklogByArea implements Backlog {
  List<QuantityAtArea> quantityByArea;
}
