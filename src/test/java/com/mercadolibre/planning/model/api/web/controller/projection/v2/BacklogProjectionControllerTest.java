package com.mercadolibre.planning.model.api.web.controller.projection.v2;

import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.projection.dto.response.BacklogProjectionResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = BacklogProjectionController.class)
class BacklogProjectionControllerTest {

    private static final String URL_V2 = "/logistic_center/{logisticCenterId}/projections/backlog";

    @InjectMocks
    private BacklogProjectionController backlogProjectionController;


    @Autowired
    private MockMvc mvc;

    @Test
    public void testGetCalculationBacklogProjection() throws Exception {

        //WHEN
        final ResultActions result = mvc.perform(
                post(URL_V2, "ARTW01")
                        .contentType(APPLICATION_JSON)
                        .content(getResourceAsString("post_backlog_projection.json"))
        );

        //THEN
        result.andExpect(status().isOk())
                .andExpect(content().json(getResourceAsString("requests/backlog/backlog_projection_response.json")));

    }

    @Test
    public void testGetCalculationBacklogProjectionFailUrl() throws Exception {

        // Perform the request and verify the response
        final ResultActions result = mvc.perform(post(URL_V2 + "/backlog1", "ARTW01")
                .contentType(APPLICATION_JSON)
                .content(getResourceAsString("post_backlog_projection.json")));

        // You can also perform additional assertions to verify the response body or headers
        result.andExpect(status().is4xxClientError());

    }

    @Test
    void testGetCalculationProjectionWhenBacklogIsNull() {
        // Arrange
        String logisticCenterId = "ARTW01";

        // Act
        ResponseEntity<List<BacklogProjectionResponse>>
                responseEntity = backlogProjectionController.getCalculationProjection(logisticCenterId, null);

        // Assert
        assertEquals("Response status code should be 400", HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

}
