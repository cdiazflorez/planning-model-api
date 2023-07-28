package com.mercadolibre.planning.model.api.web.controller.deferral;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.SaveDeferralReport;
import com.newrelic.api.agent.Trace;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/deferred")
public class DeferralController {

  private SaveDeferralReport saveDeferralReport;

  @PostMapping("/save")
  @Trace(dispatcher = true)
  public ResponseEntity<Response> save(
      @RequestBody @Valid final Msg request) {

    saveDeferralReport.save(request.getMsg().getWarehouseId(), request.getMsg().getLastUpdated(),
        mapSlaDeferral(request.getMsg().getProjections()));

    return ResponseEntity.status(HttpStatus.CREATED).body(new Response("Save"));
  }

  private List<SaveDeferralReport.SlaDeferredReport> mapSlaDeferral(final List<Msg.Projection> projections) {
    return projections.stream()
        .map(projection -> new SaveDeferralReport.SlaDeferredReport(
            projection.getEstimatedTimeDeparture(),
            projection.getDeferralStatus().getDeferralType()))
        .collect(Collectors.toList());
  }

  @Value
  @AllArgsConstructor
  private static class Response {
    String message;
  }
}
