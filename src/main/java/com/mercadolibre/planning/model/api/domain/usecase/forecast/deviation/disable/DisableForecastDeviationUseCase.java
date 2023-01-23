package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable;

import static java.time.ZonedDateTime.now;
import static java.util.Set.of;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import java.util.List;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DisableForecastDeviationUseCase implements UseCase<DisableForecastDeviationInput, Integer> {

  private final CurrentForecastDeviationRepository deviationRepository;

  @Override
  @Transactional
  public Integer execute(final DisableForecastDeviationInput input) {
    final List<CurrentForecastDeviation> warehouseDeviations =
        deviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowIn(input.getWarehouseId(), of(input.getWorkflow()));

    warehouseDeviations.forEach(
        currentForecastDeviation -> {
          currentForecastDeviation.setActive(false);
          currentForecastDeviation.setLastUpdated(now());
        }
    );

    deviationRepository.saveAll(warehouseDeviations);

    return warehouseDeviations.size();
  }

}
