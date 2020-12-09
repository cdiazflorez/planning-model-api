package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetRemainingProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetEntityInput;
import com.mercadolibre.planning.model.api.web.controller.request.Source;

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
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetBacklogTargetOutput;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BacklogTargetController.class)
public class BacklogTargetControllerTest {

    private static final String URL = "/planning/model/workflows/{workflow}/backlog_target";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GetRemainingProcessingUseCase getRemainingProcessingUseCase;

    @DisplayName("Get backlog target works ok")
    @ParameterizedTest
    @MethodSource("getParameters")
    public void testGetBacklogTargetOk(final MultiValueMap<String, String> params)
            throws Exception {
        // GIVEN
        when(getRemainingProcessingUseCase.execute(any(GetEntityInput.class)))
                .thenReturn(mockGetBacklogTargetOutput());

        // WHEN
        final ResultActions result = mvc.perform(
                get(URL, "fbm-wms-outbound")
                        .contentType(APPLICATION_JSON)
                        .params(params)
        );

        // THEN
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString("get_backlog_target_response.json")));
    }

    private static Stream<Arguments> getParameters() {
        final LinkedMultiValueMap<String, String> withDateInTo = new LinkedMultiValueMap<>();
        withDateInTo.add("warehouse_id", WAREHOUSE_ID);
        withDateInTo.add("date_from", A_DATE_UTC.toString());
        withDateInTo.add("date_to", A_DATE_UTC.plusDays(2).toString());
        withDateInTo.add("source", Source.SIMULATION.name());
        withDateInTo.add("process_name", ProcessName.WAVING.name());

       
        return Stream.of(Arguments.of(withDateInTo));
    }
}
