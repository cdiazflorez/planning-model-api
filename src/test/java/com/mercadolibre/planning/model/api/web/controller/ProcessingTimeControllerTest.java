package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeOutput;
import com.mercadolibre.planning.model.api.domain.usecase.processingtime.create.CreateProcessingTimeUseCase;
import com.mercadolibre.planning.model.api.exception.BadRequestException;
import com.mercadolibre.planning.model.api.web.controller.processingtime.ProcessingTimeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.time.ZonedDateTime.parse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProcessingTimeController.class)
public class ProcessingTimeControllerTest {

    private static final String URL = "/planning/model/configuration/processing_time/save";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CreateProcessingTimeUseCase createProcessingTimeUseCase;

    @Test
    public void saveProcessingTimeOk() throws Exception {

        // GIVEN
        final ZonedDateTime dateFrom = parse("2021-01-01T11:00:00Z[UTC]");
        final ZonedDateTime dateTo = parse("2021-01-02T10:00:00Z[UTC]");
        final long id = 1L;

        // WHEN
        when(createProcessingTimeUseCase.execute(
                CreateProcessingTimeInput.builder()
                        .value(360)
                        .metricUnit(MetricUnit.MINUTES)
                        .logisticCenterId("ARBA01")
                        .workflow(FBM_WMS_OUTBOUND)
                        .cptFrom(dateFrom)
                        .cptTo(dateTo)
                        .userId(1234)
                        .build())
        ).thenReturn(CreateProcessingTimeOutput.builder()
                .id(id)
                .build());

        final ResultActions result = mvc.perform(post(URL)
                .contentType(APPLICATION_JSON)
                .content(getResourceAsString("post_processing_time.json"))
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    public void saveProcessingTimeError() throws Exception {

        // GIVEN
        when(createProcessingTimeUseCase.execute(any(CreateProcessingTimeInput.class)))
                .thenThrow(BadRequestException.class);

        // WHEN
        final ResultActions result = mvc.perform(post(URL)
                .contentType(APPLICATION_JSON));

        // THEN
        result.andExpect(status().isInternalServerError());
    }
}
