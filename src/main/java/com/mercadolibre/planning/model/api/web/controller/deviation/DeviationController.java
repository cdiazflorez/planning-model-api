package com.mercadolibre.planning.model.api.web.controller.deviation;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.disable.DisableForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.save.SaveForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.request.DisableDeviationRequest;
import com.mercadolibre.planning.model.api.web.controller.deviation.request.SaveDeviationRequest;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ZonedDateTimeEditor;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.time.ZonedDateTime;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/deviations")
public class DeviationController {

    private final SaveForecastDeviationUseCase saveDeviationUseCase;
    private final DisableForecastDeviationUseCase disableDeviationUseCase;
    private final GetForecastDeviationUseCase getForecastDeviationUseCase;

    @PostMapping("/save")
    @Trace(dispatcher = true)
    public ResponseEntity<DeviationResponse> saveForecastDeviation(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final SaveDeviationRequest request) {
        return ResponseEntity.ok(
                saveDeviationUseCase.execute(request.toDeviationInput(workflow))
        );
    }

    @PostMapping("/disable")
    @Trace(dispatcher = true)
    public ResponseEntity<DeviationResponse> disableForecastDeviation(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final DisableDeviationRequest request) {

        disableDeviationUseCase.execute(request.toDisableDeviationInput(workflow));
        return ResponseEntity.ok(new DeviationResponse(200));
    }

    @GetMapping
    @Trace(dispatcher = true)
    public ResponseEntity<GetForecastDeviationResponse> getDeviation(
            @PathVariable final Workflow workflow,
            @RequestParam final String warehouseId,
            @RequestParam final ZonedDateTime date) {
        return ResponseEntity.ok(
                getForecastDeviationUseCase.execute(
                        new GetForecastDeviationInput(warehouseId, workflow, date))
        );
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(ZonedDateTime.class, new ZonedDateTimeEditor());
    }
}
