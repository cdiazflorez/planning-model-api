package com.mercadolibre.planning.model.api.web.controller.projection.v2;

import static java.time.temporal.ChronoUnit.MINUTES;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.projection.SLAProjectionService;
import com.mercadolibre.planning.model.api.projection.builder.PackingProjectionBuilder;
import com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult;
import com.mercadolibre.planning.model.api.web.controller.projection.request.SLAsProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.response.SLAsProjectionResponse;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterId}/projections/sla")
public class SLAsProjectionController {

  private static Map<Instant, Instant> getCutOffs(final Map<Instant, Integer> cycleTimeBySla) {
    return cycleTimeBySla.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> calculateCutOffFromSla(entry.getKey(), entry.getValue())));
  }

  private static Instant calculateCutOffFromSla(final Instant sla, final Integer cycleTime) {
    return sla.minus(cycleTime, MINUTES);
  }

  @PostMapping
  @Trace(dispatcher = true)
  public ResponseEntity<SLAsProjectionResponse> getSLAsProjection(
      @PathVariable final String logisticCenterId,
      @RequestBody final SLAsProjectionRequest slAsProjection
  ) {
    final Workflow wf = Workflow.of(slAsProjection.workflow().getName()).orElseThrow();

    final SlaProjectionResult slaProjectionResult = SLAProjectionService.execute(
        slAsProjection.dateFrom(),
        slAsProjection.dateTo(),
        slAsProjection.mapBacklogs(),
        slAsProjection.mapForecast(),
        slAsProjection.mapThroughput(),
        getCutOffs(slAsProjection.cycleTimeBySla()),
        new PackingProjectionBuilder()
    );

    return ResponseEntity.ok(new SLAsProjectionResponse(wf, slaProjectionResult));
  }
}
