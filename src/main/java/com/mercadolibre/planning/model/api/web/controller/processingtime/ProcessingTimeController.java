package com.mercadolibre.planning.model.api.web.controller.processingtime;

import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeOutput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeUseCase;
import com.mercadolibre.planning.model.api.web.controller.processingtime.request.CreateProcessingTimeRequest;
import com.mercadolibre.planning.model.api.web.controller.processingtime.response.CreateProcessingTimeResponse;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/configuration/processing_time")
public class ProcessingTimeController {

    private final CreateProcessingTimeUseCase createProcessingTimeUseCase;

    @PostMapping("/save")
    @Trace(dispatcher = true)
    public ResponseEntity<CreateProcessingTimeResponse> saveProcessingTime(
            @RequestBody final CreateProcessingTimeRequest createProcessingTimeRequest) {

        final CreateProcessingTimeOutput createProcessingTimeOutput =
                createProcessingTimeUseCase.execute(createProcessingTimeRequest
                        .toCreateForecastInput(createProcessingTimeRequest));

        return ResponseEntity.ok(createProcessingTimeOutput
                .toCreateProcessingTimeResponse(createProcessingTimeOutput));
    }
}
