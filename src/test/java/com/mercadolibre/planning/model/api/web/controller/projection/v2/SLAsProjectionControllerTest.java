package com.mercadolibre.planning.model.api.web.controller.projection.v2;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.SLAProjectionService;
import com.mercadolibre.planning.model.api.projection.builder.PackingProjectionBuilder;
import com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(controllers = SLAsProjectionController.class)
public class SLAsProjectionControllerTest {
  private static final Instant DATE_1 = Instant.parse("2023-09-14T12:00:00Z");

  private static final Instant DATE_2 = Instant.parse("2023-09-14T13:00:00Z");

  private static final Instant DATE_3 = Instant.parse("2023-09-14T14:00:00Z");

  public static final SlaProjectionResult PROJECTION_RESULT = new SlaProjectionResult(
      List.of(
          new SlaProjectionResult.Sla(
              DATE_3,
              DATE_2,
              0D
          )
      )
  );

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

  private static final Map<Instant, Instant> CUT_OFF_BY_SLA = Map.of(
      DATE_1, DATE_1.minus(1, ChronoUnit.HOURS),
      DATE_2, DATE_1,
      DATE_3, DATE_2
  );

  private static MockedStatic<SLAProjectionService> mockedSettings;

  @Autowired
  private MockMvc mvc;

  @BeforeAll
  public static void init() {
    mockedSettings = mockStatic(SLAProjectionService.class);
  }

  @AfterAll
  public static void close() {
    mockedSettings.close();
  }

  public static Stream<Arguments> testArguments() throws IOException {
    return Stream.of(
        Arguments.of(
            DATE_1,
            DATE_3,
            CURRENT_BACKLOG,
            THROUGHPUT,
            CUT_OFF_BY_SLA,
            FORECAST_BACKLOG,
            getResourceAsString("controller/projection/v2/request_sla_projection_200.json"),
            PROJECTION_RESULT,
            status().isOk()
        ),
        Arguments.of(
            DATE_1,
            DATE_3,
            CURRENT_BACKLOG,
            THROUGHPUT,
            CUT_OFF_BY_SLA,
            emptyMap(),
            getResourceAsString("controller/projection/v2/request_sla_projection_empty_planning_unit_200.json"),
            PROJECTION_RESULT,
            status().isOk()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("testArguments")
  void testAvailableCapacityEndpoint(
      final Instant from,
      final Instant to,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final Map<Instant, Instant> cutOffs,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecast,
      final String request,
      final SlaProjectionResult capacity,
      final ResultMatcher responseCode
  ) throws Exception {
    when(
        SLAProjectionService.execute(
            from,
            to,
            currentBacklog,
            forecast,
            throughput,
            cutOffs,
            new PackingProjectionBuilder()
        )).thenReturn(capacity);

    // WHEN
    final String url = "/logistic_center/ARTW01/projections/sla";

    final ResultActions resultActions = mvc.perform(
        post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
    );

    // THEN
    resultActions.andExpect(responseCode);
    if (responseCode.equals(status().isOk())) {
      resultActions.andExpect(content().json(getResourceAsString("controller/projection/v2/response_sla_projection.json")));
    }
  }
}
