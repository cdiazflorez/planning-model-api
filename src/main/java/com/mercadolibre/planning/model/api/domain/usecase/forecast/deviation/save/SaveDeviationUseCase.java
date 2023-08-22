package com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.exception.UnexpiredDeviationPresentException;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SaveDeviationUseCase {

  private static final int STATUS_OK = 200;

  private final CurrentForecastDeviationRepository deviationRepository;

  @Transactional
  public DeviationResponse execute(final List<SaveDeviationInput> inputs) {
    // TODO: Remover este método cuando el flujo de IB se migre a la control-tool.
    inputs.forEach(this::saveDeviations);
    return new DeviationResponse(STATUS_OK);
  }

  /**
   * Save a list of deviations by workflow.
   * Inbound workflow: keeps original functionality
   * Outbound workflow: it validates if there are unexpired deviations, if it exists, it throws an exception, otherwise,
   * it saves the new deviation but does not inactivate the expired ones if there are any.
   *
   * @param workflow    workflow
   * @param warehouseId logistic center id
   * @param inputs      List of deviations to save
   * @param currentDate current date
   * @throws UnexpiredDeviationPresentException throws an exception in case there is an active deviation not expired.
   */
  @Transactional
  public void execute(final Workflow workflow,
                      final String warehouseId,
                      final List<SaveDeviationInput> inputs,
                      final ZonedDateTime currentDate) {

    final List<CurrentForecastDeviation> currentActiveDeviation = deviationRepository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThan(warehouseId, workflow, currentDate);
    if (!currentActiveDeviation.isEmpty()) {
      throw new UnexpiredDeviationPresentException("There is a deviation percentage in effect.");
    }

    inputs.forEach(this::saveOutboundDeviations);
  }

  private void saveDeviations(final SaveDeviationInput input) {
    // TODO: Remover este método cuando el flujo de IB se migre a la control-tool.
    final List<CurrentForecastDeviation> forecastDeviations = buildForecastDeviationsToSave(input);
    deviationRepository.disableDeviation(input.getWarehouseId(), input.getWorkflow(), input.getDeviationType(), input.getPaths());
    deviationRepository.saveAll(forecastDeviations);
  }

  private void saveOutboundDeviations(final SaveDeviationInput input) {
    final List<CurrentForecastDeviation> forecastDeviations = buildForecastDeviationsToSave(input);
    deviationRepository.saveAll(forecastDeviations);
  }

  private List<CurrentForecastDeviation> buildForecastDeviationsToSave(final SaveDeviationInput input) {
    return input.getPaths() != null
        ? input.getPaths().stream()
        .map(path -> CurrentForecastDeviation
            .builder()
            .logisticCenterId(input.getWarehouseId())
            .dateFrom(input.getDateFrom())
            .dateTo(input.getDateTo())
            .value(input.getValue())
            .isActive(true)
            .userId(input.getUserId())
            .workflow(input.getWorkflow())
            .type(input.getDeviationType())
            .path(path)
            .build())
        .collect(Collectors.toList())
        : List.of(input.toCurrentForecastDeviation());
  }
}
