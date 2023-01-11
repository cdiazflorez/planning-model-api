package com.mercadolibre.planning.model.api.web.controller.suggestionwaves;

import com.mercadolibre.planning.model.api.projection.Suggestion;
import com.mercadolibre.planning.model.api.projection.SuggestionsUseCase;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.request.SuggestionsWavesRequestDto;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.SuggestionWavesDto;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/flow/logistic_center/{logisticCenterId}/projections")
public class SuggestionWavesController {

    private final SuggestionsUseCase suggestionUseCase;

    @PostMapping("/waves")
    public ResponseEntity<SuggestionWavesDto> getSuggestions(
            @PathVariable final String logisticCenterId,
            final @RequestBody SuggestionsWavesRequestDto request) {
        List<Suggestion> suggestedWaves = suggestionUseCase.execute(
                request.getProcessPathConfigurations(),
                request.getBacklogs(),
                request.getThroughputRatios(),
                request.getBacklogLimits(),
                request.getViewDate()
        );
        return ResponseEntity.of(Optional.of(new SuggestionWavesDto(logisticCenterId, request.getViewDate(), suggestedWaves)));
    }
}

