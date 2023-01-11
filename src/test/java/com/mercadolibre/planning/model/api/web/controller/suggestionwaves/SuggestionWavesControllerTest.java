package com.mercadolibre.planning.model.api.web.controller.suggestionwaves;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.Suggestion;
import com.mercadolibre.planning.model.api.projection.SuggestionsUseCase;
import com.mercadolibre.planning.model.api.projection.UnitsByDateOut;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import com.mercadolibre.planning.model.api.projection.Wave;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.request.SuggestionsWavesRequestDto;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = SuggestionWavesController.class)
class SuggestionWavesControllerTest {
    public static final Instant VIEW_DATE = Instant.parse("2022-12-16T16:00:00Z");
    private static final String URL = "/flow/logistic_center/{logisticCenterId}/projections";
    @Autowired
    private MockMvc mvc;

    @MockBean
    private SuggestionsUseCase suggestionUseCase;

    @DisplayName("Get suggested waves ok")
    @Test
    public void testGetSuggestedWavesOk() throws Exception {
        // GIVEN
        when(
                suggestionUseCase.execute(
                        getCtByPP(),
                        getBacklogs(),
                        getRatios(),
                        getBacklogsLimits(),
                        VIEW_DATE
                ))
                .thenReturn(getSuggestedWaves());

        // WHEN
        final ResultActions resultActions = mvc.perform(
                post(URL + "/waves", "ARTW01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new JSONObject()
                                .put("request", getRequest())
                                .toString()
                        )
        );

        // THEN
        resultActions.andExpect(status().isOk());
    }

    private SuggestionsWavesRequestDto getRequest() {
        return new SuggestionsWavesRequestDto(VIEW_DATE, getCtByPP(), getBacklogs(), getRatios(), getBacklogsLimits());
    }

    private Map<ProcessPath, Map<Instant, Float>> getRatios() {
        return Map.of(
                ProcessPath.TOT_MONO, Map.of(VIEW_DATE, 0.5F),
                ProcessPath.TOT_MULTI_BATCH, Map.of(VIEW_DATE, 0.25F),
                ProcessPath.TOT_MULTI_ORDER, Map.of(VIEW_DATE, 0.25F));
    }

    private Map<ProcessName, Map<Instant, Integer>> getBacklogsLimits() {
        return Map.of(
                ProcessName.PICKING, Map.of(VIEW_DATE, 10000, VIEW_DATE.plus(5, ChronoUnit.HOURS), 50000),
                ProcessName.PACKING, Map.of(VIEW_DATE, 2000),
                ProcessName.BATCH_SORTER, Map.of(VIEW_DATE, 700)
        );
    }

    private List<ProcessPathConfiguration> getCtByPP() {
        return List.of(new ProcessPathConfiguration(ProcessPath.NON_TOT_MONO, 250, 200, 50));
    }

    private List<UnitsByProcessPathAndProcess> getBacklogs() {
        return List.of(
                new UnitsByProcessPathAndProcess(
                        ProcessPath.NON_TOT_MONO,
                        ProcessName.WAVING,
                        Instant.parse("2022-12-16T20:00:00Z"),
                        1000)
        );
    }

    private List<Suggestion> getSuggestedWaves() {
        var boundsByPP = new Wave(ProcessPath.NON_TOT_MONO, 250, 15000, new TreeSet<>(
                Collections.singleton(Instant.parse("2022-12-16T20:00:00Z")))
        );
        var unitsByDateOut = new UnitsByDateOut(Instant.parse("2022-12-16T20:00:00Z"), 250);
        return List.of(
                new Suggestion(
                        VIEW_DATE,
                        List.of(boundsByPP),
                        TriggerName.SLA,
                        List.of(unitsByDateOut)
                )
        );
    }

}
