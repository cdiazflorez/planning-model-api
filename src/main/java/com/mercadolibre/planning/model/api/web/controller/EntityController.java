package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetEntityOutput;
import com.mercadolibre.planning.model.api.exception.InvalidEntityTypeException;
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

import static java.util.Collections.emptyList;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/entities")
public class EntityController {

    private final GetHeadcountEntityUseCase getHeadcountEntityUseCase;
    private final GetProductivityEntityUseCase getProductivityEntityUseCase;

    @GetMapping("/{entityType}")
    public ResponseEntity<List<GetEntityOutput>> getEntity(
            @PathVariable final Workflow workflow,
            @PathVariable final EntityType entityType,
            @Valid final GetEntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow, entityType);

        if (entityType == EntityType.HEADCOUNT) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(getHeadcountEntityUseCase.execute(input));
        } else if (entityType == EntityType.PRODUCTIVITY) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(getProductivityEntityUseCase.execute(input));
        } else if (entityType == EntityType.THROUGHPUT) {
            return ResponseEntity.status(HttpStatus.OK)
                    .body(emptyList());
        } else {
            throw new InvalidEntityTypeException(entityType);
        }
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
