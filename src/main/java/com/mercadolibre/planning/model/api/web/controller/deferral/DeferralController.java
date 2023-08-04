package com.mercadolibre.planning.model.api.web.controller.deferral;

import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveOutboundDeferralReport;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/deferred")
public class DeferralController {

  private SaveOutboundDeferralReport saveOutboundDeferralReport;

  @PostMapping("/save")
  @Trace(dispatcher = true)
  public ResponseEntity<DeferralResponse> save(
      @RequestBody @Valid final Msg request
  ) {

    final DeferralResponse deferralResponse = saveOutboundDeferralReport.save(
        request.getMsg().getWarehouseId(),
        request.getMsg().getLastUpdated(),
        mapSlaDeferral(request.getMsg().getProjections())
    );

    return ResponseEntity.status(deferralResponse.getStatus()).body(deferralResponse);
  }

  private List<SaveOutboundDeferralReport.CptDeferralReport> mapSlaDeferral(final List<Msg.Projection> projections) {
    return projections.stream()
        .map(projection -> new SaveOutboundDeferralReport.CptDeferralReport(
            projection.getEstimatedTimeDeparture(),
            true,
            projection.getDeferralStatus().getDeferralType()))
        .collect(Collectors.toList());
  }

  @Value
  @AllArgsConstructor
  private static class Response {
    String message;
  }
}
