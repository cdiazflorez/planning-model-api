package com.mercadolibre.planning.model.api.web.controller.projection.v2;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.projection.v2.backlog.BacklogUnifiedProjection;
import com.mercadolibre.planning.model.api.projection.dto.request.BacklogProjection;
import com.mercadolibre.planning.model.api.projection.dto.request.total.BacklogProjectionTotalRequest;
import com.mercadolibre.planning.model.api.projection.dto.response.Backlog;
import com.mercadolibre.planning.model.api.projection.dto.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.api.projection.dto.response.Process;
import com.mercadolibre.planning.model.api.projection.dto.response.ProcessPathResponse;
import com.mercadolibre.planning.model.api.projection.dto.response.Sla;
import com.mercadolibre.planning.model.api.projection.dto.response.total.BacklogProjectionTotalResponse;
import com.mercadolibre.planning.model.api.projection.dto.response.total.SlaTotal;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterId}/projections/backlog")
@Slf4j
public class BacklogProjectionController {
  private static final int IP_INTERVAL_SIZE = 60;

  private final BacklogUnifiedProjection backlogUnifiedProjection;

  /**
   * Method that handles the POST request for /backlog. Performs a projection calculation
   * and returns a list of ProjectionBacklogResponse objects.
   *
   * @param logisticCenterId  The ID of the logistic center to which the request refers.
   * @param backlogProjection The backlog projection request to be processed.
   * @return A ResponseEntity object containing a list of ProjectionBacklogResponse objects.
   * @throws IllegalArgumentException if backlogProjection is null.
   */
  @PostMapping
  @Trace(dispatcher = true)
  public ResponseEntity<List<BacklogProjectionResponse>> getCalculationProjection(
      @PathVariable final String logisticCenterId,
      @RequestBody final BacklogProjection backlogProjection) {

    final Instant dateOut = Instant.parse("2023-04-10T14:00:00Z");
    final Instant operationHour = Instant.parse("2023-04-10T10:00:00Z");

    final BacklogProjectionResponse backlogProjectionResponse = new BacklogProjectionResponse(
        operationHour,
        List.of(
            new Backlog(
                List.of(
                    new Process(
                        ProcessName.PICKING,
                        List.of(
                            new Sla(
                                dateOut,
                                List.of(
                                    new ProcessPathResponse(
                                        TOT_MONO,
                                        50
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    );

    return ResponseEntity.ok(List.of(backlogProjectionResponse));
  }

  /**
   * Method that handles the POST request for /total. Performs a projection calculation
   * and returns a list of BacklogProjectionTotalResponse objects.
   *
   * @param logisticCenterId The ID of the logistic center to which the request refers.
   * @param request          The backlog projection request to be processed.
   * @return A ResponseEntity object containing a list of BacklogProjectionTotalResponse objects.
   */
  @PostMapping("/total")
  @Trace(dispatcher = true)
  public ResponseEntity<List<BacklogProjectionTotalResponse>> getCalculationProjectionUnified(
      @PathVariable final String logisticCenterId,
      @RequestBody @Valid final BacklogProjectionTotalRequest request) {

    request.validateDateRange();

    final var projection = backlogUnifiedProjection.getProjection(request, IP_INTERVAL_SIZE);

    final var response = projection.entrySet().stream()
        .map(dateByProjection ->
            new BacklogProjectionTotalResponse(dateByProjection.getKey(),
                toSlaTotal(dateByProjection.getValue())
            )
        ).collect(Collectors.toList());

    return ResponseEntity.ok(response);
  }

  private List<SlaTotal> toSlaTotal(final Map<Instant, Map<ProcessPath, Long>> backlogByDateOut) {
    return backlogByDateOut.entrySet().stream()
        .map(backlogBySla -> new SlaTotal(backlogBySla.getKey(),
            sumProcessPath(backlogBySla.getValue()),
            toProcessPathResponse(backlogBySla.getValue())
        )).collect(Collectors.toList());
  }

  private int sumProcessPath(final Map<ProcessPath, Long> backlogByProcessPath) {
    return backlogByProcessPath.entrySet().stream().flatMapToInt(backlogByPP -> IntStream.of(Math.toIntExact(backlogByPP.getValue())))
        .sum();
  }

  private List<ProcessPathResponse> toProcessPathResponse(final Map<ProcessPath, Long> backlogByProcessPath) {
    return backlogByProcessPath.entrySet().stream()
        .map(backlogByPP -> new ProcessPathResponse(backlogByPP.getKey(), Math.toIntExact(backlogByPP.getValue())))
        .collect(Collectors.toList());
  }

}
