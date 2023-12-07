package com.mercadolibre.planning.model.api.web.controller.availablecapacity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.availablecapacity.AvailableCapacityUseCase;
import com.mercadolibre.planning.model.api.projection.availablecapacity.CapacityBySLA;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(controllers = AvailableCapacityController.class)
class AvailableCapacityControllerTest {
  private static final Instant DATE_1 = Instant.parse("2023-09-14T12:00:00Z");
  private static final Instant DATE_2 = Instant.parse("2023-09-14T13:00:00Z");
  public static final List<CapacityBySLA> AVAILABLE_CAPACITY = List.of(new CapacityBySLA(DATE_1, 0), new CapacityBySLA(DATE_2, 0));
  private static final Instant DATE_3 = Instant.parse("2023-09-14T14:00:00Z");
  private static final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> FORECAST_BACKLOG = Map.of(
      DATE_1, Map.of(
          TOT_MONO, Map.of(
              DATE_3, 50L
          )
      )
  );
  private static final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> CURRENT_BACKLOG = Map.of(
      ProcessName.PICKING, Map.of(
          TOT_MONO, Map.of(
              DATE_3, 150L
          )
      ),
      ProcessName.PACKING, Map.of(
          TOT_MONO, Map.of(
              DATE_3, 150L
          )
      )
  );
  private static final Map<ProcessName, Map<Instant, Integer>> THROUGHPUT = Map.of(
      ProcessName.PICKING, Map.of(
          DATE_1, 10000,
          DATE_2, 15000,
          DATE_3, 1500
      ),
      ProcessName.PACKING, Map.of(
          DATE_1, 10000,
          DATE_2, 15000,
          DATE_3, 1500
      )
  );

  private static final Map<Instant, Integer> CYCLE_TIME_BY_SLA = Map.of(
      DATE_1, 30,
      DATE_2, 30,
      DATE_3, 30
  );

  @MockBean
  private AvailableCapacityUseCase availableCapacityUseCase;

  @Autowired
  private MockMvc mvc;

  public static Stream<Arguments> testCases() throws IOException {
    return Stream.of(
        Arguments.of(
            DATE_1,
            DATE_3,
            CURRENT_BACKLOG,
            THROUGHPUT,
            CYCLE_TIME_BY_SLA,
            FORECAST_BACKLOG,
            getResourceAsString("controller/slowshipments/request.json"),
            AVAILABLE_CAPACITY,
            status().isOk()
        ),
        Arguments.of(
            DATE_1,
            DATE_3,
            CURRENT_BACKLOG,
            THROUGHPUT,
            CYCLE_TIME_BY_SLA,
            emptyMap(),
            getResourceAsString("controller/slowshipments/empty_forecast_request.json"),
            AVAILABLE_CAPACITY,
            status().isOk()
        ),
        Arguments.of(
            DATE_1,
            DATE_3,
            CURRENT_BACKLOG,
            THROUGHPUT,
            CYCLE_TIME_BY_SLA,
            emptyMap(),
            getResourceAsString("controller/slowshipments/null_forecast_request.json"),
            AVAILABLE_CAPACITY,
            status().isOk()
        ),
        Arguments.of(
            null,
            null,
            null,
            null,
            null,
            null,
            getResourceAsString("controller/slowshipments/empty_request.json"),
            null,
            status().isBadRequest()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testAvailableCapacityEndpoint(
      final Instant from,
      final Instant to,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final Map<Instant, Integer> cycleTimeBySla,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecast,
      final String request,
      final List<CapacityBySLA> capacity,
      final ResultMatcher responseCode
  ) throws Exception {
    when(
        availableCapacityUseCase.execute(
            from,
            to,
            currentBacklog,
            forecast,
            throughput,
            cycleTimeBySla
        )).thenReturn(capacity);

    // WHEN
    String url = "/logistic_center/ARTW01/projections/capacity";

    final ResultActions resultActions = mvc.perform(
        post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
    );

    // THEN
    resultActions.andExpect(responseCode);
    if (responseCode.equals(status().isOk())) {
      resultActions.andExpect(content().json(getResourceAsString("controller/slowshipments/response.json")));
    }
  }
}
