package com.mercadolibre.planning.model.api.web.controller.entity;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.SearchEntitiesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.performedprocessing.get.GetPerformedProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.remainingprocessing.get.GetRemainingProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessingTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.SourceEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.entity.request.EntityRequest;
import com.mercadolibre.planning.model.api.web.controller.entity.request.HeadcountRequest;
import com.mercadolibre.planning.model.api.web.controller.entity.request.ProductivityRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.request.EntitySearchRequest;
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
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.OK;

@RestController
@AllArgsConstructor
@SuppressWarnings("PMD.ExcessiveImports")
@RequestMapping("/planning/model/workflows/{workflow}/entities")
public class EntityController {

    private final GetHeadcountEntityUseCase getHeadcountUseCase;
    private final GetProductivityEntityUseCase getProductivityUseCase;
    private final GetThroughputUseCase getThroughputUseCase;
    private final GetRemainingProcessingUseCase getRemainingProcessingUseCase;
    private final GetPerformedProcessingUseCase getPerformedProcessingUseCase;
    private final SearchEntitiesUseCase searchEntitiesUseCase;

    @GetMapping("/headcount")
    @Trace(dispatcher = true)
    public ResponseEntity<List<EntityOutput>> getHeadcounts(
            @PathVariable final Workflow workflow,
            @Valid final HeadcountRequest request) {

        final GetHeadcountInput input = request.toGetHeadcountInput(workflow);
        return ResponseEntity.status(OK).body(getHeadcountUseCase.execute(input));
    }

    @GetMapping("/productivity")
    @Trace(dispatcher = true)
    public ResponseEntity<List<ProductivityOutput>> getProductivity(
            @PathVariable final Workflow workflow,
            @Valid final ProductivityRequest request) {

        final GetProductivityInput input = request.toGetProductivityInput(workflow);
        return ResponseEntity.status(OK).body(getProductivityUseCase.execute(input));
    }

    @GetMapping("/throughput")
    @Trace(dispatcher = true)
    public ResponseEntity<List<EntityOutput>> getThroughput(
            @PathVariable final Workflow workflow,
            @Valid final EntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow);
        return ResponseEntity.status(OK).body(getThroughputUseCase.execute(input));
    }

    @GetMapping("/performed_processing")
    @Trace(dispatcher = true)
    public ResponseEntity<List<EntityOutput>> getPerformedProcessing(
            @PathVariable final Workflow workflow,
            @Valid final EntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow);
        return ResponseEntity.status(OK).body(getPerformedProcessingUseCase.execute(input));
    }

    @PostMapping("/search")
    @Trace(dispatcher = true)
    public ResponseEntity<Map<EntityType,Object>> searchEntities(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final EntitySearchRequest request) {
        return ResponseEntity.ok(searchEntitiesUseCase.execute(request.toSearchInput(workflow))
        );
    }

    @PostMapping("/headcount")
    @Trace(dispatcher = true)
    public ResponseEntity<List<EntityOutput>> getHeadcountsWithSimulations(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final HeadcountRequest request) {

        final GetHeadcountInput input = request.toGetHeadcountInput(workflow);
        return ResponseEntity.status(OK).body(getHeadcountUseCase.execute(input));
    }

    @PostMapping("/productivity")
    @Trace(dispatcher = true)
    public ResponseEntity<List<ProductivityOutput>> getProductivityWithSimulations(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final ProductivityRequest request) {

        final GetProductivityInput input = request.toGetProductivityInput(workflow);
        return ResponseEntity.status(OK).body(getProductivityUseCase.execute(input));
    }

    @PostMapping("/throughput")
    @Trace(dispatcher = true)
    public ResponseEntity<List<EntityOutput>> getThroughputWithSimulations(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final EntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow);
        return ResponseEntity.status(OK).body(getThroughputUseCase.execute(input));
    }

    @PostMapping("/remaining_processing")
    public ResponseEntity<List<EntityOutput>> getRemainingProcessing(
            @PathVariable final Workflow workflow,
            @RequestBody @Valid final EntityRequest request) {

        final GetEntityInput input = request.toGetEntityInput(workflow);
        return ResponseEntity.status(OK)
                .body(getRemainingProcessingUseCase.execute(input));
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
