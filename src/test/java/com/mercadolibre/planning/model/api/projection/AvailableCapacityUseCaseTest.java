package com.mercadolibre.planning.model.api.projection;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.availablecapacity.AvailableCapacityUseCase;
import com.mercadolibre.planning.model.api.projection.availablecapacity.CapacityBySLA;
import com.mercadolibre.planning.model.api.projection.builder.Projector;
import com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AvailableCapacityUseCaseTest {

  private static final Instant DATE_1 = Instant.parse("2023-09-12T10:00:00Z");
  private static final Instant DATE_2 = Instant.parse("2023-09-12T13:00:00Z");
  private static final Instant SLA_1 = Instant.parse("2023-09-12T13:00:00Z");
  private static final Instant SLA_2 = Instant.parse("2023-09-12T14:00:00Z");
  private static final Instant SLA_3 = Instant.parse("2023-09-12T15:00:00Z");
  private static final Instant SLA_4 = Instant.parse("2023-09-12T16:00:00Z");
  private static final Instant END_DATE_1 = Instant.parse("2023-09-12T12:00:00Z");
  private static final Instant END_DATE_2 = Instant.parse("2023-09-12T13:00:00Z");
  private static final Instant END_DATE_3 = Instant.parse("2023-09-12T14:00:00Z");
  private static final Instant END_DATE_4 = Instant.parse("2023-09-12T15:00:00Z");
  private static final double ZERO = 0D;
  private static final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> CURRENT_BACKLOG = Map.of(
      PICKING, Map.of(TOT_MONO, Map.of(SLA_1, 1L))
  );
  private static final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> FORECAST_BACKLOG = Map.of(
      DATE_1, Map.of(TOT_MONO, Map.of(SLA_1, 0L)));
  private static final Map<ProcessName, Map<Instant, Integer>> THROUGHPUT = Map.of(
      WAVING, Map.of(DATE_1, 500, END_DATE_1, 3000, END_DATE_2, 400, END_DATE_3, 200, END_DATE_4, 100),
      PICKING, Map.of(DATE_1, 1000, END_DATE_1, 1000, END_DATE_2, 400, END_DATE_3, 200, END_DATE_4, 80),
      PACKING, Map.of(DATE_1, 1500, END_DATE_1, 200, END_DATE_2, 200, END_DATE_3, 200, END_DATE_4, 100),
      PACKING_WALL, Map.of(DATE_1, 500, END_DATE_1, 200, END_DATE_2, 200, END_DATE_3, 200, END_DATE_4, 100),
      BATCH_SORTER, Map.of(DATE_1, 50, END_DATE_1, 2000, END_DATE_2, 200, END_DATE_3, 200, END_DATE_4, 100),
      WALL_IN, Map.of(DATE_1, 100, END_DATE_1, 2000, END_DATE_2, 200, END_DATE_3, 200, END_DATE_4, 100)
  );
  private static final Map<Instant, Integer> CYCLE_TIME_BY_SLA = Map.of(SLA_1, 30, SLA_2, 30, SLA_3, 30, SLA_4, 30);
  private static final Map<Instant, Instant> CUT_OFF_BY_SLA = Map.of(
      SLA_1, SLA_1.minus(30, ChronoUnit.MINUTES),
      SLA_2, SLA_2.minus(30, ChronoUnit.MINUTES),
      SLA_3, SLA_3.minus(30, ChronoUnit.MINUTES),
      SLA_4, SLA_4.minus(30, ChronoUnit.MINUTES)
  );
  private static MockedStatic<SLAProjectionService> mockedSettings;

  @InjectMocks
  private AvailableCapacityUseCase availableCapacityUseCase;


  @BeforeAll
  public static void init() {
    mockedSettings = mockStatic(SLAProjectionService.class);
  }

  @AfterAll
  public static void close() {
    mockedSettings.close();
  }

  public static Stream<Arguments> testCases() {
    return Stream.of(
        Arguments.of(
            new SlaProjectionResult(List.of(new SlaProjectionResult.Sla(SLA_1, END_DATE_1, ZERO))),
            List.of(new CapacityBySLA(SLA_1, 200))
        ),
        Arguments.of(
            new SlaProjectionResult(List.of(
                new SlaProjectionResult.Sla(SLA_1, END_DATE_1, ZERO),
                new SlaProjectionResult.Sla(SLA_2, END_DATE_2, ZERO),
                new SlaProjectionResult.Sla(SLA_3, END_DATE_3, ZERO),
                new SlaProjectionResult.Sla(SLA_4, END_DATE_4, ZERO)
            )),
            List.of(new CapacityBySLA(SLA_1, 40), new CapacityBySLA(SLA_2, 40), new CapacityBySLA(SLA_3, 40), new CapacityBySLA(SLA_4, 40))
        ),
        Arguments.of(
            new SlaProjectionResult(List.of(
                new SlaProjectionResult.Sla(SLA_1, END_DATE_1, ZERO),
                new SlaProjectionResult.Sla(SLA_2, END_DATE_2, ZERO),
                new SlaProjectionResult.Sla(SLA_3, null, 1D),
                new SlaProjectionResult.Sla(SLA_4, END_DATE_4, ZERO)
            )),
            List.of(new CapacityBySLA(SLA_1, 0), new CapacityBySLA(SLA_2, 0), new CapacityBySLA(SLA_3, 0), new CapacityBySLA(SLA_4, 40))
        ),
        Arguments.of(
            new SlaProjectionResult(List.of(
                new SlaProjectionResult.Sla(SLA_1, null, 55),
                new SlaProjectionResult.Sla(SLA_2, END_DATE_2, ZERO),
                new SlaProjectionResult.Sla(SLA_3, END_DATE_3, ZERO),
                new SlaProjectionResult.Sla(SLA_4, END_DATE_4, ZERO)
            )),
            List.of(
                new CapacityBySLA(SLA_1, 0),
                new CapacityBySLA(SLA_2, 40),
                new CapacityBySLA(SLA_3, 40),
                new CapacityBySLA(SLA_4, 40))
        ),
        Arguments.of(
            new SlaProjectionResult(List.of(
                new SlaProjectionResult.Sla(SLA_1, END_DATE_1, ZERO),
                new SlaProjectionResult.Sla(SLA_2, null, 500),
                new SlaProjectionResult.Sla(SLA_3, END_DATE_3, ZERO),
                new SlaProjectionResult.Sla(SLA_4, END_DATE_4, ZERO)
            )),
            List.of(
                new CapacityBySLA(SLA_1, 0),
                new CapacityBySLA(SLA_2, 0),
                new CapacityBySLA(SLA_3, 40),
                new CapacityBySLA(SLA_4, 40))
        )
    );
  }

  @ParameterizedTest
  @MethodSource("testCases")
  void testAvailableCapacity(
      final SlaProjectionResult mockProjectionService,
      final List<CapacityBySLA> expectedCapacity
  ) {
    when(SLAProjectionService.execute(
        eq(DATE_1),
        eq(DATE_2),
        eq(CURRENT_BACKLOG),
        eq(FORECAST_BACKLOG),
        eq(THROUGHPUT),
        eq(CUT_OFF_BY_SLA),
        any(Projector.class)
    )).thenReturn(mockProjectionService);

    final var obtainedCapacity = availableCapacityUseCase.execute(
        DATE_1,
        DATE_2,
        CURRENT_BACKLOG,
        FORECAST_BACKLOG,
        THROUGHPUT,
        CYCLE_TIME_BY_SLA
    );

    // THEN
    assertEquals(expectedCapacity, obtainedCapacity);
  }
}
