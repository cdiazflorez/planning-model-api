package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockInputOptimization;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.InputOptimizationService;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.request.InputOptimizationRequest;
import com.mercadolibre.planning.model.api.web.controller.inputoptimization.InputOptimizationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = InputOptimizationController.class)
public class InputOptimizationControllerTest {

    private static final String URL = "/planning/model/inputs";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private InputOptimizationService inputOptimizationService;

    @Test
    @DisplayName("Get input optimization OK")
    public void testInputOptimizationOk() throws Exception {
        //GIVEN
        when(inputOptimizationService.getInputOptimization(any(InputOptimizationRequest.class))).thenReturn(mockInputOptimization());
        //WHEN
        final ResultActions resultActions = mvc.perform(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getResourceAsString("inputoptimization/request/post_input_optimization_request.json"))
        );
        //THEN
        resultActions.andExpect(status().isOk());
    }

}
