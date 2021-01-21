package com.mercadolibre.planning.model.api.web.controller.suggestedwave;

import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.GetSuggestedWavesInput;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.GetSuggestedWavesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.suggestedwave.get.SuggestedWavesOutput;
import com.mercadolibre.planning.model.api.web.controller.editor.ProjectionTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WaveCardinalityEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/projections/suggested_waves")
public class SuggestedWavesController {

    private final GetSuggestedWavesUseCase getSuggestedWavesUseCase;

    @GetMapping
    public ResponseEntity<List<SuggestedWavesOutput>> getSuggestedWaves(
            @PathVariable final Workflow workflow,
            @Valid final GetSuggestedWavesRequest request) {
        final GetSuggestedWavesInput input = request.getSuggestedWavesInput(workflow);
        final List<SuggestedWavesOutput> totalQuantity = getSuggestedWavesUseCase.execute(input);

        return ResponseEntity.status(HttpStatus.OK)
                .body(totalQuantity);
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(WaveCardinality.class, new WaveCardinalityEditor());
        dataBinder.registerCustomEditor(ProjectionType.class, new ProjectionTypeEditor());
    }
}
