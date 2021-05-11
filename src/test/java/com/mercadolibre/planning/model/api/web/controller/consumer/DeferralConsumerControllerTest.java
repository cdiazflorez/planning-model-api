package com.mercadolibre.planning.model.api.web.controller.consumer;

import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralCptUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralInput;
import com.mercadolibre.planning.model.api.web.consumer.DeferralConsumerController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeferralConsumerController.class)
public class DeferralConsumerControllerTest {

    private static final String URL = "/feed/deferral";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private DeferralCptUseCase deferralCptUseCase;

    @Test
    public void testDeferralFeed() throws Exception {
        // GIVEN

        // WHEN
        final ResultActions result = mvc.perform(post(URL)
                .contentType(APPLICATION_JSON)
                .content(getResourceAsString("deferralBQMessage.json"))
        );

        // THEN
        result.andExpect(status().isOk());

        verify(deferralCptUseCase).execute(new DeferralInput("ARBA01", FBM_WMS_OUTBOUND));
    }
}
