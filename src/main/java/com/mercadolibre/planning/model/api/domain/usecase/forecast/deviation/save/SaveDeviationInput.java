package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class SaveDeviationInput {

  private static final double TOTAL_PERCENTAGE = 0.01;

  String warehouseId;

  ZonedDateTime dateFrom;

  ZonedDateTime dateTo;

  double value;

  Long userId;

  Workflow workflow;

  DeviationType deviationType;

  List<Path> paths;

  public CurrentForecastDeviation toCurrentForecastDeviation() {
    return CurrentForecastDeviation
        .builder()
        .logisticCenterId(warehouseId)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .value(calculateValueByWorkflow(workflow, value))
        .isActive(true)
        .userId(userId)
        .workflow(workflow)
        .type(deviationType)
        .build();
  }

  /**
   * If it is outbound, the value multiplied by TOTAL_PERCENTAGE is calculated,
   * otherwise the value is sent to the {@link CurrentForecastDeviationRepository} as it is received,
   * this is to maintain backward compatibility, so it should be removed.
   * @param workflow workflow in which the value should be calculated
   * @param value in decimal value
   * @return the value that will be sent to the repository depending on the workflow
   */
  private double calculateValueByWorkflow(final Workflow workflow, final double value) {
    if (workflow.equals(Workflow.FBM_WMS_OUTBOUND)) {
      return value * TOTAL_PERCENTAGE;
    }
    return value;
  }

}
