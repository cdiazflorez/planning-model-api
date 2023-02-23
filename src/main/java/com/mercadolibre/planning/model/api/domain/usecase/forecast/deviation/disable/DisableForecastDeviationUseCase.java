package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable;

import static java.time.ZonedDateTime.now;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class DisableForecastDeviationUseCase {

  private final CurrentForecastDeviationRepository deviationRepository;

  @Transactional
  public Integer execute(final List<DisableForecastDeviationInput> input) {


    final List<AdjustmentByPath> adjustmentByPaths = input.stream()
        .map(this::mapPaths)
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());

    final List<CurrentForecastDeviation> activeAdjustment = adjustmentByPaths.stream().map(adjustmentByPath ->
        deviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndType(
            adjustmentByPath.logisticCenterId,
            Set.of(adjustmentByPath.workflow),
            adjustmentByPath.path,
            adjustmentByPath.deviationType)
    ).flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());

    activeAdjustment.forEach(
        currentForecastDeviation -> {
          currentForecastDeviation.setActive(false);
          currentForecastDeviation.setLastUpdated(now());
        }
    );
    deviationRepository.saveAll(activeAdjustment);

    return activeAdjustment.size();
  }

  private List<AdjustmentByPath> mapPaths(final DisableForecastDeviationInput adjustment) {
    return adjustment.getAffectedShipmentTypes() != null
        ? adjustment.getAffectedShipmentTypes().stream()
            .map(path -> new AdjustmentByPath(adjustment.getWorkflow(), adjustment.getWarehouseId(), adjustment.getDeviationType(), path))
            .collect(Collectors.toUnmodifiableList())
        : List.of(new AdjustmentByPath(adjustment.getWorkflow(), adjustment.getWarehouseId(), adjustment.getDeviationType(), null));
  }

  @AllArgsConstructor
  static class AdjustmentByPath {
   private Workflow workflow;
   private String logisticCenterId;
   private DeviationType deviationType;
   private Path path;
  }

}
