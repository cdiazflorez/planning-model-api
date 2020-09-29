package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetEntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.strategy.GetEntityStrategy;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.SourceEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.GetEntityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
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
@RequestMapping("/planning/model/workflows/{workflow}/entities")
public class EntityController {

    private final GetEntityStrategy getEntityStrategy;

    @GetMapping("/{entityType}")
    public ResponseEntity<List<GetEntityOutput>> getEntity(
            @PathVariable final Workflow workflow,
            @PathVariable final EntityType entityType,
            @Valid final GetEntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow, entityType);
        return ResponseEntity.status(HttpStatus.OK)
                .body(getEntityStrategy.getBy(entityType).execute(input));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(EntityType.class, new EntityTypeEditor());
        dataBinder.registerCustomEditor(MetricUnit.class, new MetricUnitEditor());
        dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
        dataBinder.registerCustomEditor(Source.class, new SourceEditor());
    }
}
