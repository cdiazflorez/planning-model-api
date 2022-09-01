package com.mercadolibre.planning.model.api.web.controller.entity;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME;
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
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.GetMaxCapacityByWarehouseEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.GetMaxCapacityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.search.SearchEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.ConvertUtils;
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
import java.io.ByteArrayInputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@SuppressWarnings("PMD.ExcessiveImports")
@RequestMapping("/planning/model/workflows/{workflow}/entities")
public class EntityController {

  private final SearchEntitiesUseCase searchEntitiesUseCase;

  private final GetHeadcountEntityUseCase getHeadcountUseCase;

  private final GetProductivityEntityUseCase getProductivityUseCase;

  private final GetThroughputUseCase getThroughputUseCase;

  private final SearchEntityUseCase searchEntityUseCase;

  private final GetMaxCapacityEntityUseCase getMaxCapacityEntityUseCase;

  private final GetMaxCapacityByWarehouseEntityUseCase getMaxCapacityByWarehouseEntityUseCase;

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

    final GetEntityInput input =
        request.toGetEntityInput(workflow, EntityType.PERFORMED_PROCESSING);

    return ResponseEntity.status(OK).body(searchEntityUseCase.execute(input));
  }

  @PostMapping("/search")
  @Trace(dispatcher = true)
  public ResponseEntity<Map<EntityType, Object>> searchEntities(
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

    final GetEntityInput input =
        request.toGetEntityInput(workflow, EntityType.REMAINING_PROCESSING);

    return ResponseEntity.status(OK).body(searchEntityUseCase.execute(input));
  }

  @GetMapping(value = "/max_capacity", produces = "text/csv")
  public ResponseEntity<?> getMaxCapacity(
      @PathVariable final Workflow workflow,
      @RequestParam @DateTimeFormat(iso = DATE_TIME) final ZonedDateTime dateFrom,
      @RequestParam @DateTimeFormat(iso = DATE_TIME) final ZonedDateTime dateTo) {

    final String csvFile = ConvertUtils.toCsvFile(
        getMaxCapacityEntityUseCase.execute(workflow, dateFrom, dateTo));


    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; "
            + "filename=MaxCapacityFile.csv")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(new InputStreamResource(new ByteArrayInputStream(csvFile.getBytes(UTF_8))));
  }

  @GetMapping("/tph")
  public ResponseEntity<?> getTphMaxCapacity(
      @RequestParam final String warehouse,
      @RequestParam @DateTimeFormat(iso = DATE_TIME) final ZonedDateTime dateFrom,
      @RequestParam @DateTimeFormat(iso = DATE_TIME) final ZonedDateTime dateTo) {


    return ResponseEntity.ok()
        .body(getMaxCapacityByWarehouseEntityUseCase.execute(warehouse, dateFrom, dateTo));
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
