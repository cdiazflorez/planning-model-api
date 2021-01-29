package com.mercadolibre.planning.model.api.web.controller.deviation;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.SaveForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.request.DeviationRequest;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.DeviationResponse;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/deviations")
public class DeviationController {

    private final SaveForecastDeviationUseCase saveForecastDeviationUseCase;

    @PostMapping("/save")
    @Trace(dispatcher = true)
    public ResponseEntity<DeviationResponse> saveForecastDeviation(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final DeviationRequest request) {
        return ResponseEntity.ok(
                saveForecastDeviationUseCase.execute(request.toDeviationInput(workflow))
        );
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    }
}
