package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.AbilityLevelEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.HeadcountTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.StaffingPlanGroupersEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanUpdateRequest;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
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

  private final StaffingPlanAdapter staffingPlanAdapter;

  private final UpdateStaffingPlanUseCase updateStaffingPlanUseCase;

  @GetMapping
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
  }
}
