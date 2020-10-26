package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.CalculateProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.strategy.CalculateProjectionStrategy;
import com.mercadolibre.planning.model.api.exception.ProjectionTypeNotSupportedException;
import com.mercadolibre.planning.model.api.web.controller.editor.ProjectionTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.request.ProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.ProjectionType;
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

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/projections")
public class ProjectionController {

    private final CalculateProjectionStrategy calculateProjectionStrategy;

    @PostMapping
    @Trace(dispatcher = true)
    public ResponseEntity<List<ProjectionOutput>> getProjection(
            @PathVariable final Workflow workflow,
            @RequestBody final ProjectionRequest request) {

        final ProjectionType projectionType = request.getType();
        final CalculateProjectionUseCase calculateProjectionUseCase = calculateProjectionStrategy
                .getBy(projectionType)
                .orElseThrow(() -> new ProjectionTypeNotSupportedException(projectionType));

        return ResponseEntity
                .ok(calculateProjectionUseCase.execute(request.toProjectionInput(workflow)));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(ProjectionType.class, new ProjectionTypeEditor());
    }
}
