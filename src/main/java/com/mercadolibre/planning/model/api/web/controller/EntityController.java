package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.output.ProductivityOutput;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessingTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.SourceEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.request.EntityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.HeadcountRequest;
import com.mercadolibre.planning.model.api.web.controller.request.ProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
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

import static org.springframework.http.HttpStatus.OK;

@RestController
@AllArgsConstructor
@SuppressWarnings("PMD.ExcessiveImports")
@RequestMapping("/planning/model/workflows/{workflow}/entities")
public class EntityController {

    private final GetHeadcountEntityUseCase getHeadcountUseCase;
    private final GetProductivityEntityUseCase getProductivityUseCase;
    private final GetThroughputUseCase getThroughputUseCase;

    @GetMapping("/headcount")
    public ResponseEntity<List<EntityOutput>> getHeadcounts(
            @PathVariable final Workflow workflow,
            @Valid final HeadcountRequest request) {

        final GetHeadcountInput input = request.toGetHeadcountInput(workflow);
        return ResponseEntity.status(OK).body(getHeadcountUseCase.execute(input));
    }

    @GetMapping("/productivity")
    public ResponseEntity<List<ProductivityOutput>> getProductivity(
            @PathVariable final Workflow workflow,
            @Valid final ProductivityRequest request) {

        final GetProductivityInput input = request.toGetProductivityInput(workflow);
        return ResponseEntity.status(OK).body(getProductivityUseCase.execute(input));
    }

    @GetMapping("/throughput")
    public ResponseEntity<List<EntityOutput>> getThroughput(
            @PathVariable final Workflow workflow,
            @Valid final EntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow);
        return ResponseEntity.status(OK).body(getThroughputUseCase.execute(input));
    }

    //TODO: Unificar estos 3 post en una sola llamada
    @PostMapping("/headcount")
    public ResponseEntity<List<EntityOutput>> getHeadcountsWithSimulations(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final HeadcountRequest request) {

        final GetHeadcountInput input = request.toGetHeadcountInput(workflow);
        return ResponseEntity.status(OK).body(getHeadcountUseCase.execute(input));
    }

    @PostMapping("/productivity")
    public ResponseEntity<List<ProductivityOutput>> getProductivityWithSimulations(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final ProductivityRequest request) {

        final GetProductivityInput input = request.toGetProductivityInput(workflow);
        return ResponseEntity.status(OK).body(getProductivityUseCase.execute(input));
    }

    @PostMapping("/throughput")
    public ResponseEntity<List<EntityOutput>> getThroughputWithSimulations(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final EntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow);
        return ResponseEntity.status(OK).body(getThroughputUseCase.execute(input));
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
