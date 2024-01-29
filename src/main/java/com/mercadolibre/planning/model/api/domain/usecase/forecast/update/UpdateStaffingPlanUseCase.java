package com.mercadolibre.planning.model.api.domain.usecase.forecast.update;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UpdateStaffingPlanUseCase {

  private final CurrentProcessingDistributionGateway currentProcessingDistributionGateway;

  public void execute(final UpdateStaffingPlanInput updateStaffingPlanInput) {
    deactivateOldUpdates(updateStaffingPlanInput);
    createUpdate(updateStaffingPlanInput);
  }

  private void createUpdate(final UpdateStaffingPlanInput input) {
    final var simulationInputs = input.resources().stream()
        .flatMap(resource -> resource.values().stream()
            .map(values -> toCreateSimulationInput(input, resource, values)))
        .toList();
    currentProcessingDistributionGateway.createStaffingUpdates(simulationInputs);
  }

  private CreateSimulationInput toCreateSimulationInput(
      final UpdateStaffingPlanInput input,
      final UpdateStaffingPlanInput.Resource resource,
      final UpdateStaffingPlanInput.ResourceValues values
  ) {
    return new CreateSimulationInput(
        input.workflow(),
        input.logisticCenterId(),
        input.userId(),
        values.tags(),
        values.value(),
        values.date(),
        resource.name()
    );

  }

  private void deactivateOldUpdates(final UpdateStaffingPlanInput input) {
    input.resources()
        .forEach(
            resource -> resource
                .values()
                .forEach(values -> currentProcessingDistributionGateway.deactivateStaffingUpdate(
                        input.workflow(),
                        input.logisticCenterId(),
                        values.date(),
                        values.tags() == null ? Map.of() : values.tags(),
                        input.userId(),
                        resource.name().getProcessingType(),
                        resource.name().getMetricUnit()
                    )
                )
        );
  }

  /**
   * Gateway to interact with the current processing distribution repository.
   */
  public interface CurrentProcessingDistributionGateway {

    void createStaffingUpdates(List<CreateSimulationInput> input);

    void deactivateStaffingUpdate(
        Workflow workflow,
        String logisticCenterId,
        ZonedDateTime date,
        Map<String, String> tags,
        long userId,
        ProcessingType type,
        MetricUnit metricUnit
    );
  }

  public record CreateSimulationInput(
      Workflow workflow,
      String logisticCenterId,
      long userId,
      Map<String, String> tags,
      double quantity,
      ZonedDateTime date,
      EntityType type
  ) {
  }
}
