package com.mercadolibre.planning.model.api.web.controller.deferral;

import static com.mercadolibre.planning.model.api.util.DateUtils.validateDatesRanges;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.GetDeferralReport;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveOutboundDeferralReport;
import com.mercadolibre.planning.model.api.exception.DateRangeException;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

  @PostMapping("/event")
  @Trace(dispatcher = true)
  public ResponseEntity<DeferralResponse> saveDeferredEvent(
      @RequestBody @Valid final Msg request
  ) {

    final DeferralResponse deferralResponse = saveOutboundDeferralReport.save(
        request.getMsg().getWarehouseId(),
        request.getMsg().getLastUpdated(),
        mapSlaDeferral(request.getMsg().getProjections())
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
                .collect(Collectors.toList())))
        .collect(Collectors.toList());

    return ResponseEntity.ok(new DeferralReportDto(deferralTime));
  }

  private List<SaveOutboundDeferralReport.CptDeferralReport> mapSlaDeferral(final List<Msg.Projection> projections) {
    return projections.stream()
        .map(projection -> new SaveOutboundDeferralReport.CptDeferralReport(
            projection.getEstimatedTimeDeparture(),
            true,
            projection.getDeferralStatus().getDeferralType()))
        .collect(Collectors.toList());
  }
}
