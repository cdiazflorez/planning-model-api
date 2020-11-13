package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.GetEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.strategy.GetEntityStrategy;
import com.mercadolibre.planning.model.api.exception.EntityTypeNotSupportedException;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessingTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.SourceEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.request.EntityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;

@SuppressWarnings("PMD.ExcessiveImports")
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/entities")
public class EntityController {

    private final GetEntityStrategy getEntityStrategy;

    @GetMapping("/{entityType}")
    public ResponseEntity<List<EntityOutput>> getEntity(
            @PathVariable final Workflow workflow,
            @PathVariable final EntityType entityType,
            @Valid final EntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow, entityType);
        final GetEntityUseCase getEntityUseCase = getEntityStrategy
                .getBy(entityType)
                .orElseThrow(() -> new EntityTypeNotSupportedException(entityType));

        return ResponseEntity.status(HttpStatus.OK).body(getEntityUseCase.execute(input));
    }

    @PostMapping("/{entityType}")
    public ResponseEntity<List<EntityOutput>> getEntityWithSimulations(
            @PathVariable final Workflow workflow,
            @PathVariable final EntityType entityType,
            @RequestBody @Valid final EntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow, entityType);
        final GetEntityUseCase getEntityUseCase = getEntityStrategy
                .getBy(entityType)
                .orElseThrow(() -> new EntityTypeNotSupportedException(entityType));

        return ResponseEntity.status(HttpStatus.OK).body(getEntityUseCase.execute(input));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(EntityType.class, new EntityTypeEditor());
        dataBinder.registerCustomEditor(MetricUnit.class, new MetricUnitEditor());
        dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
        dataBinder.registerCustomEditor(Source.class, new SourceEditor());
        dataBinder.registerCustomEditor(ProcessingType.class, new ProcessingTypeEditor());
    }
}
