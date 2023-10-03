package com.mercadolibre.planning.model.api.web.controller.deferral;

import static com.mercadolibre.planning.model.api.util.DateUtils.validateDatesRanges;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.GetDeferralReport;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.GetDeferred;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.GetDeferred.DeferralStatus;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveOutboundDeferralReport;
import com.mercadolibre.planning.model.api.exception.DateRangeException;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
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
@RequestMapping("/planning/model/deferred")
public class DeferralController {

  private SaveOutboundDeferralReport saveOutboundDeferralReport;

  private GetDeferralReport getDeferralReport;

  private GetDeferred getDeferred;

  @PostMapping("/events")
  @Trace(dispatcher = true)
  public ResponseEntity<DeferralResponse> saveDeferredEvent(@RequestBody @Valid final Msg request) {
    final DeferralResponse deferralResponse = saveOutboundDeferralReport.save(
        request.getMsg().getWarehouseId(),
        request.getMsg().getLastUpdated(),
        mapSlaDeferral(request.getMsg().getCompletedProjections())
    );

    return ResponseEntity.status(deferralResponse.getStatus()).body(deferralResponse);
  }

  @GetMapping
  @Trace(dispatcher = true)
  public ResponseEntity<DeferralReportDto> get(
      @RequestParam @Valid @NotNull @NotBlank final String logisticCenterId,
      @RequestParam @Valid @NotNull final Instant dateFrom,
      @RequestParam @Valid @NotNull final Instant dateTo
  ) {
    if (!validateDatesRanges(dateFrom, dateTo)) {
      throw new DateRangeException(dateFrom, dateTo);
    }
    final var deferralReport = getDeferralReport.execute(logisticCenterId, dateFrom, dateTo);

    final List<DeferralReportDto.DeferralTime> deferralTime = deferralReport.entrySet().stream()
        .map(deferral -> new DeferralReportDto.DeferralTime(
            deferral.getKey(),
            deferral.getValue().stream()
                .map(sla -> new DeferralReportDto.DeferralTime.StatusBySla(sla.getDate(), sla.getStatus()))
                .toList())
        ).toList();

    return ResponseEntity.ok(new DeferralReportDto(deferralTime));
  }

  @Trace(dispatcher = true)
  @GetMapping("{logisticCenterId}/status")
  public ResponseEntity<DeferralStatusDto> getDeferralsAtDate(
      @PathVariable final String logisticCenterId,
      @RequestParam @Valid @NotNull final Workflow workflow,
      @RequestParam @Valid @NotNull final Instant viewDate
  ) {
    final var statuses = getDeferred.getDeferred(logisticCenterId, workflow, viewDate);
    final var response = new DeferralStatusDto(
        statuses.stream()
            .sorted(Comparator.comparing(DeferralStatus::date))
            .toList()
    );

    return ResponseEntity.ok(response);
  }

  private List<SaveOutboundDeferralReport.CptDeferralReport> mapSlaDeferral(final List<Msg.Projection> projections) {
    return projections.stream()
        .map(projection -> new SaveOutboundDeferralReport.CptDeferralReport(
            projection.getEstimatedTimeDeparture(),
            projection.isUpdated(),
            projection.getDeferralStatus().getDeferralType())
        )
        .toList();
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
  }

  record DeferralStatusDto(List<DeferralStatus> statuses) {
  }

}
