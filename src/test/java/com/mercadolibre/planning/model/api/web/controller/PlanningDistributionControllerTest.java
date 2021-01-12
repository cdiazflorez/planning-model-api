package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.web.controller.planningdistribution.PlanningDistributionController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.stream.Stream;

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

    private static final String URL = "/planning/model/workflows/{workflow}/planning_distributions";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetPlanningDistributionUseCase getPlanningDistributionUseCase;

    @DisplayName("Get planning distribution works ok")
    @ParameterizedTest
    @MethodSource("getParameters")
    public void testGetPlanningDistributionOk(final MultiValueMap<String, String> params)
            throws Exception {
        // GIVEN
        when(getPlanningDistributionUseCase.execute(any(GetPlanningDistributionInput.class)))
                .thenReturn(mockGetPlanningDistOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .params(params)
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString("get_processing_distribution.json")));
    }

    private static Stream<Arguments> getParameters() {
        final LinkedMultiValueMap<String, String> withDateInTo = new LinkedMultiValueMap<>();
        withDateInTo.add("warehouse_id", WAREHOUSE_ID);
        withDateInTo.add("date_out_from", A_DATE_UTC.toString());
        withDateInTo.add("date_out_to", A_DATE_UTC.plusDays(2).toString());
        withDateInTo.add("date_in_to", A_DATE_UTC.minusDays(2).toString());

        final LinkedMultiValueMap<String, String> withoutDateInTo = new LinkedMultiValueMap<>();
        withoutDateInTo.add("warehouse_id", WAREHOUSE_ID);
        withoutDateInTo.add("date_out_from", A_DATE_UTC.toString());
        withoutDateInTo.add("date_out_to", A_DATE_UTC.plusDays(2).toString());

        return Stream.of(Arguments.of(withDateInTo), Arguments.of(withoutDateInTo));
    }
}
