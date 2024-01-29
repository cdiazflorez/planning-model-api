package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.HEADCOUNT;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toMap;
import static org.springframework.http.HttpStatus.OK;

import com.google.common.base.CaseFormat;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanResponse;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.plan.staffing.GetStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.AbilityLevelEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.HeadcountTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessingTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.StaffingPlanGroupersEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanUpdateRequest;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterId}/plan/staffing/v2")
public class StaffingPlanController {

  private static final List<String> ADDITIONAL_FILTERS_TO_REMOVE =
      List.of("logisticCenterId", "resource", "workflow", "dateFrom", "dateTo", "viewDate", "groupers");

  private static final String COMMA_SEPARATOR = ",";

  private final StaffingPlanAdapter staffingPlanAdapter;

  private final UpdateStaffingPlanUseCase updateStaffingPlanUseCase;

  private final GetStaffingPlanUseCase getStaffingPlanUseCase;

  /**
   * @param logisticCenterId the logistic center id.
   * @param workflow the workflow.
   * @param resources the resources.
   * @param groupers the groupers.
   * @param processPaths the process paths.
   * @param processes the processes.
   * @param abilityLevels the ability levels.
   * @param headcountTypes the headcount types.
   * @param dateFrom the date from.
   * @param dateTo the date to.
   * @param viewDate the view date.
   * @return the staffing plan.
   * @deprecated use {@link #getStaffingPlan(String, ProcessingType, Workflow, Instant, Instant, Instant, List, Map)}.
   */
  @GetMapping
  @Deprecated
  @Trace(dispatcher = true)
  public ResponseEntity<StaffingPlanAdapter.StaffingPlanResponse> getStaffingPlan(
      @PathVariable final String logisticCenterId,
      @RequestParam final Workflow workflow,
      @RequestParam final List<EntityType> resources,
      @RequestParam(required = false) final List<StaffingPlanRequest.Groupers> groupers,
      @RequestParam(required = false) final List<String> processPaths,
      @RequestParam(required = false) final List<ProcessName> processes,
      @RequestParam(required = false) final List<StaffingPlanRequest.AbilityLevel> abilityLevels,
      @RequestParam(required = false) final List<StaffingPlanRequest.HeadcountType> headcountTypes,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo,
      @RequestParam final Instant viewDate
  ) {

    return ResponseEntity.status(OK).body(staffingPlanAdapter.getStaffingPlan(
        new StaffingPlanRequest(
            resources,
            workflow,
            ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC),
            ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC),
            viewDate,
            logisticCenterId,
            groupers == null ? List.of() : groupers,
            processPaths,
            abilityLevels == null ? List.of() : abilityLevels,
            headcountTypes == null ? List.of() : headcountTypes,
            processes
        )
    ));
  }

  @GetMapping("/{resource}")
  public ResponseEntity<List<StaffingPlanResponse>> getStaffingPlan(
      @PathVariable final String logisticCenterId,
      @PathVariable final ProcessingType resource,
      @RequestParam final Workflow workflow,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo,
      @RequestParam final Instant viewDate,
      @RequestParam(required = false) final List<String> groupers,
      @RequestParam(required = false) final Map<String, String> filters
  ) {
    final Instant newViewDate = viewDate == null ? Instant.now() : viewDate;
    final List<String> newGroupers = groupers == null ? new ArrayList<>() : groupers;
    final Map<String, List<String>> newFilters = filters == null ? emptyMap() : cleanFilters(filters);

    //TODO: remove this when HEADCOUNT is ready and EFFECTIVE_WORKERS and EFFECTIVE_WORKERS_NS are no longer persisted
    if (resource == HEADCOUNT) {

      final List<StaffingPlanResponse> response = mergeStaffingPlan(
          getStaffingPlanUseCase.execute(
              logisticCenterId,
              workflow,
              EFFECTIVE_WORKERS,
              newGroupers,
              newFilters,
              dateFrom,
              dateTo,
              newViewDate
          ),
          getStaffingPlanUseCase.execute(
              logisticCenterId,
              workflow,
              EFFECTIVE_WORKERS_NS,
              newGroupers,
              newFilters,
              dateFrom,
              dateTo,
              newViewDate
          )
      );

      return ResponseEntity.status(OK).body(response);

    }

    return ResponseEntity.status(OK)
        .body(getStaffingPlanUseCase.execute(
                  logisticCenterId,
                  workflow,
                  resource,
                  newGroupers,
                  newFilters,
                  dateFrom,
                  dateTo,
                  newViewDate
              )
        );
  }

  @ResponseStatus(OK)
  @PutMapping
  @Trace(dispatcher = true)
  public void updateStaffingPlan(
      @PathVariable final String logisticCenterId,
      @RequestParam final Workflow workflow,
      @RequestParam final long userId,
      @RequestBody final StaffingPlanUpdateRequest request
  ) {
    final UpdateStaffingPlanInput input = request.toUpdateStaffingPlanInput(
        logisticCenterId,
        workflow,
        userId
    );
    updateStaffingPlanUseCase.execute(input);
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(EntityType.class, new EntityTypeEditor());
    dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
    dataBinder.registerCustomEditor(StaffingPlanRequest.Groupers.class, new StaffingPlanGroupersEditor());
    dataBinder.registerCustomEditor(StaffingPlanRequest.AbilityLevel.class, new AbilityLevelEditor());
    dataBinder.registerCustomEditor(StaffingPlanRequest.HeadcountType.class, new HeadcountTypeEditor());
    dataBinder.registerCustomEditor(ProcessingType.class, new ProcessingTypeEditor());
  }

  private static String toSnakeCase(final String value) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, value);
  }

  private Map<String, List<String>> cleanFilters(final Map<String, String> filters) {
    return filters.entrySet().stream()
        .filter(entry -> !ADDITIONAL_FILTERS_TO_REMOVE.contains(entry.getKey()))
        .collect(
            toConcurrentMap(
                key -> toSnakeCase(key.getKey()),
                value -> Arrays.stream(value.getValue().split(COMMA_SEPARATOR)).toList()
            )
        );
  }

  private List<StaffingPlanResponse> mergeStaffingPlan(final List<StaffingPlanResponse> staffingPlanResponses,
                                                       final List<StaffingPlanResponse> anotherStaffingPlanResponses) {

    final var staffingGrouper = staffingPlanResponses.stream()
        .collect(
            toMap(
                StaffingPlanResponse::groupers,
                value -> value
            )
        );
    final var anotherStaffingGrouper = anotherStaffingPlanResponses.stream()
        .collect(
            toMap(
                StaffingPlanResponse::groupers,
                value -> value
            )
        );
    final var keysStream = Stream.concat(
            staffingGrouper.keySet().stream(),
            anotherStaffingGrouper.keySet().stream()
        )
        .distinct();

    return keysStream.map(
            key -> new StaffingPlanResponse(
                Optional.ofNullable(staffingGrouper.get(key)).map(StaffingPlanResponse::value).orElse(0D)
                    + Optional.ofNullable(anotherStaffingGrouper.get(key)).map(StaffingPlanResponse::value).orElse(0D),
                Optional.ofNullable(staffingGrouper.get(key)).map(StaffingPlanResponse::originalValue).orElse(0D)
                    + Optional.ofNullable(anotherStaffingGrouper.get(key)).map(StaffingPlanResponse::originalValue).orElse(0D),
                key
            )
        )
        .toList();
  }

}
