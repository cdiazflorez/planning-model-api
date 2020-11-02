package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.usecase.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetPlanningDistOutput;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlanningDistributionController.class)
public class PlanningDistributionControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/planning_distribution";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetPlanningDistributionUseCase getPlanningDistributionUseCase;

    @DisplayName("Get planning distribution works ok")
    @Test
    public void testGetPlanningDistributionOk() throws Exception {
        // GIVEN
        when(getPlanningDistributionUseCase.execute(any(GetPlanningDistributionInput.class)))
                .thenReturn(mockGetPlanningDistOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .param("warehouse_id", WAREHOUSE_ID)
                        .param("date_from", A_DATE_UTC.toString())
                        .param("date_to", A_DATE_UTC.plusDays(2).toString())
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString("get_processing_distribution.json")));
    }
}
