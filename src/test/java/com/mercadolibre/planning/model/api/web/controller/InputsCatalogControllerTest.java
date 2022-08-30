package com.mercadolibre.planning.model.api.web.controller;

import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.SHIFTS_PARAMETERS;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockInputsCatalog;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.InputCatalogService;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.get.GetInputCatalog;
import com.mercadolibre.planning.model.api.web.controller.inputcatalog.InputsCatalogController;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = InputsCatalogController.class)
public class InputsCatalogControllerTest {

    private static final String URL = "/planning/model/inputs";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private InputCatalogService inputCatalogService;

    @Test
    @DisplayName("Get input optimization OK")
    public void testInputOptimizationOk() throws Exception {
        //GIVEN
        when(inputCatalogService.getInputsCatalog(any(GetInputCatalog.class))).thenReturn(mockInputsCatalog());
        //WHEN
        final ResultActions resultActions = mvc.perform(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getResourceAsString("inputcatalog/request/post_input_catalog_request.json"))
        );
        //THEN
        resultActions.andExpect(status().isOk());
    }

    @Test
    @DisplayName("Get input optimization with filters OK")
    public void testInputOptimizationWithFilterOk() throws Exception {
        //GIVEN
        when(inputCatalogService.getInputsCatalog(any(GetInputCatalog.class)))
                .thenReturn(Map.of(SHIFTS_PARAMETERS, List.of()));
        //WHEN
        final ResultActions resultActions = mvc.perform(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getResourceAsString("inputcatalog/request/post_input_catalog_request_with_filters.json"))
        );
        //THEN
        resultActions.andExpect(status().isOk());
    }

}
