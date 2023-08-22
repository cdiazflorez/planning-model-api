package com.mercadolibre.planning.model.api.web.controller.deviation;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static java.lang.Math.round;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveDeviationUseCase;
import com.mercadolibre.planning.model.api.exception.DeviationsToSaveNotFoundException;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import com.mercadolibre.planning.model.api.exception.InvalidDateRangeDeviationsException;
import com.mercadolibre.planning.model.api.exception.InvalidDateToSaveDeviationException;
import com.mercadolibre.planning.model.api.web.controller.deviation.request.DisabledDeviationAdjustmentsRequest;
import com.mercadolibre.planning.model.api.web.controller.deviation.request.SaveDeviationRequest;
import com.mercadolibre.planning.model.api.web.controller.deviation.request.SaveDeviationsContentRequest;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
import com.mercadolibre.planning.model.api.web.controller.editor.DeviationTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ZonedDateTimeEditor;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/deviations")
public class DeviationController {

  private static final int ROUNDING_FACTOR = 100;

  private static final long STATUS_OK = 200;

  private final SaveDeviationUseCase saveDeviationUseCase;

  private final DisableForecastDeviationUseCase disableDeviationUseCase;

  private final GetForecastDeviationUseCase getForecastDeviationUseCase;


  @PostMapping("/save/all")
  @Trace(dispatcher = true)
  public ResponseEntity<DeviationResponse> saveAll(
      @PathVariable final Workflow workflow,
      @RequestBody @Valid final List<SaveDeviationRequest> request) {

    final var input = request.stream()
        .map(SaveDeviationRequest::toDeviationInput)
        .collect(Collectors.toList());

    return ResponseEntity.ok(
        saveDeviationUseCase.execute(input)
    );
  }

  @PostMapping
  @Trace(dispatcher = true)
  public ResponseEntity<DeviationResponse> saveDeviations(
      @PathVariable final Workflow workflow,
      @RequestBody @Valid final SaveDeviationsContentRequest request) {

    if (request.getDeviations().isEmpty()) {
      throw new DeviationsToSaveNotFoundException();
    }

    final ZonedDateTime currentDate = Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(ZoneOffset.UTC);
    validateDateRangeOfDeviations(request.getDeviations());
    validateIfCurrentDateMustBeGreaterThanDateRange(request.getDeviations(), currentDate.toInstant());

    final List<SaveDeviationInput> input = request.getDeviations().stream()
        .map(deviation -> deviation.toDeviationInput(workflow, request.getLogisticCenterId()))
        .collect(Collectors.toList());

    saveDeviationUseCase.execute(workflow, request.getLogisticCenterId(), input, currentDate);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/disable/all")
  @Trace(dispatcher = true)
  public ResponseEntity<DeviationResponse> disableActiveForecastAdjustments(
      @PathVariable final Workflow workflow,
      @RequestParam final String logisticCenterId,
      @RequestBody final List<DisabledDeviationAdjustmentsRequest> request) {

    final var disableForecastDeviations = request.stream().map(adjustmentRequest ->
        adjustmentRequest.toDisableDeviationInput(logisticCenterId)).collect(Collectors.toUnmodifiableList());

    disableDeviationUseCase.execute(disableForecastDeviations);

    return ResponseEntity.ok(new DeviationResponse(STATUS_OK));
  }

  @GetMapping("/active")
  @Trace(dispatcher = true)
  public ResponseEntity<List<GetForecastDeviationResponse>> getActiveDeviations(
      @PathVariable final Workflow workflow,
      @RequestParam final String warehouseId,
      @RequestParam final List<Workflow> workflows,
      @RequestParam final ZonedDateTime date
  ) {
    final List<GetForecastDeviationResponse> forecastAdjustments =
        workflows.stream()
            .map(a -> getForecastDeviationUseCase.execute(new GetForecastDeviationInput(warehouseId, a, date)))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    return ResponseEntity.ok(forecastAdjustments);
  }

  @GetMapping
  @Trace(dispatcher = true)
  public ResponseEntity<GetForecastDeviationResponse> getDeviation(
      @PathVariable final Workflow workflow,
      @RequestParam final String warehouseId,
      @RequestParam final ZonedDateTime date) {

    return ResponseEntity.ok(
        getForecastDeviationUseCase.execute(new GetForecastDeviationInput(warehouseId, workflow, date))
            .stream()
            .map(deviation ->
                GetForecastDeviationResponse.builder()
                    .workflow(deviation.getWorkflow())
                    .dateFrom(deviation.getDateFrom())
                    .dateTo(deviation.getDateTo())
                    .value(round((deviation.getValue() * ROUNDING_FACTOR)))
                    .metricUnit(PERCENTAGE)
                    .path(deviation.getPath())
                    .type(deviation.getType())
                    .build()
            )
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("CurrentForecastDeviation", warehouseId))
    );
  }

  private void validateIfCurrentDateMustBeGreaterThanDateRange(final List<SaveDeviationRequest> request, final Instant currentDate) {
    request.stream()
        .filter(deviation -> currentDate.isAfter(deviation.getDateFrom().toInstant()))
        .findAny()
        .map(invalid -> {
          throw new InvalidDateToSaveDeviationException(
              invalid.getDateFrom().toInstant(),
              invalid.getDateTo().toInstant(),
              currentDate
          );
        });
  }

  private void validateDateRangeOfDeviations(final List<SaveDeviationRequest> request) {
    request.stream()
        .filter(deviation -> !deviation.getDateFrom().toInstant().isBefore(deviation.getDateTo().toInstant()))
        .findAny()
        .map(invalid -> {
          throw new InvalidDateRangeDeviationsException(
              invalid.getDateFrom().toInstant(),
              invalid.getDateTo().toInstant()
          );
        });
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ZonedDateTime.class, new ZonedDateTimeEditor());
    dataBinder.registerCustomEditor(DeviationType.class, new DeviationTypeEditor());
  }

}
