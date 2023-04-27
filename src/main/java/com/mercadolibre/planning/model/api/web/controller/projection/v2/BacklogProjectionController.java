package com.mercadolibre.planning.model.api.web.controller.projection.v2;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.dto.request.BacklogProjection;
import com.mercadolibre.planning.model.api.projection.dto.response.Backlog;
import com.mercadolibre.planning.model.api.projection.dto.response.BacklogProjectionResponse;
import com.mercadolibre.planning.model.api.projection.dto.response.Process;
import com.mercadolibre.planning.model.api.projection.dto.response.ProcessPathResponse;
import com.mercadolibre.planning.model.api.projection.dto.response.Sla;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.List;
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

        if (backlogProjection == null) {
            return ResponseEntity.badRequest().build();
        }

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
                                                                                ProcessPath.TOT_MONO,
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

}
