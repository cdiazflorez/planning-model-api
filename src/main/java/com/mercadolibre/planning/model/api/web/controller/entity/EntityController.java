package com.mercadolibre.planning.model.api.web.controller.entity;

import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.SearchEntitiesUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessingTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.SourceEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.entity.request.EntityRequest;
import com.mercadolibre.planning.model.api.web.controller.entity.request.HeadcountRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.request.EntitySearchRequest;
import com.newrelic.api.agent.Trace;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
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

@Deprecated
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/entities")
public class EntityController {

  private final SearchEntitiesUseCase searchEntitiesUseCase;

  private final GetHeadcountEntityUseCase getHeadcountUseCase;

  private final GetThroughputUseCase getThroughputUseCase;

  @GetMapping("/headcount")
  @Trace(dispatcher = true)
  public ResponseEntity<List<EntityOutput>> getHeadcounts(
      @PathVariable final Workflow workflow,
      @Valid final HeadcountRequest request) {

    final GetHeadcountInput input = request.toGetHeadcountInput(workflow);
    return ResponseEntity.status(OK).body(getHeadcountUseCase.execute(input));
  }

  @PostMapping("/headcount")
  @Trace(dispatcher = true)
  public ResponseEntity<List<EntityOutput>> getHeadcountsWithSimulations(
      @PathVariable final Workflow workflow,
      @RequestBody @Valid final HeadcountRequest request) {

    final GetHeadcountInput input = request.toGetHeadcountInput(workflow);
    return ResponseEntity.status(OK).body(getHeadcountUseCase.execute(input));
  }

  @PostMapping("/search")
  @Trace(dispatcher = true)
  public ResponseEntity<Map<EntityType, Object>> searchEntities(
      @PathVariable final Workflow workflow,
      @RequestBody @Valid final EntitySearchRequest request) {
    return ResponseEntity.ok(searchEntitiesUseCase.execute(request.toSearchInput(workflow))
    );
  }

  @PostMapping("/throughput")
  @Trace(dispatcher = true)
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
