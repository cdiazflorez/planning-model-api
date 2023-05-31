package com.mercadolibre.planning.model.api.projection.v2.backlog;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.projection.v2.backlog.BacklogUnifiedProjection;
import com.mercadolibre.planning.model.api.projection.dto.request.total.BacklogProjectionTotalRequest;
import com.mercadolibre.planning.model.api.projection.dto.request.total.BacklogRequest;
import com.mercadolibre.planning.model.api.projection.dto.request.total.ProcessPathRequest;
import com.mercadolibre.planning.model.api.projection.dto.request.total.Quantity;
import com.mercadolibre.planning.model.api.projection.dto.request.total.Throughput;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BacklogUnifiedProjectionTest {

  private static final int DAYS_AMOUNT_TO_SUBTRACT = 1;

  private static final Instant DATE_FROM = Instant.parse("2023-05-22T09:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2023-05-22T12:00:00Z");

  private static final Instant DATE_OUT1 = Instant.parse("2023-05-22T10:00:00Z");

  private static final Instant DATE_OUT2 = Instant.parse("2023-05-22T11:00:00Z");

  private static final Instant DATE_OUT3 = Instant.parse("2023-05-22T12:00:00Z");

  private static final Instant DATE_IN1 = DATE_OUT1.minus(DAYS_AMOUNT_TO_SUBTRACT, HOURS);

  private static final Instant DATE_IN2 = DATE_OUT2.minus(DAYS_AMOUNT_TO_SUBTRACT, HOURS);

  private static final Instant DATE_IN3 = DATE_OUT3.minus(DAYS_AMOUNT_TO_SUBTRACT, HOURS);

  private static final List<ProcessPathRequest> FORECASTED_BACKLOG = List.of(
      new ProcessPathRequest(
          TOT_MONO,
          List.of(
              new Quantity(DATE_IN1, DATE_OUT1, 50),
              new Quantity(DATE_IN2, DATE_OUT2, 55),
              new Quantity(DATE_IN3, DATE_OUT3, 60),
              new Quantity(DATE_OUT3, DATE_OUT3, 0)
          )
      ),
      new ProcessPathRequest(
          NON_TOT_MONO,
          List.of(
              new Quantity(DATE_IN1, DATE_OUT1, 10),
              new Quantity(DATE_IN2, DATE_OUT2, 20),
              new Quantity(DATE_IN3, DATE_OUT3, 30)
          )
      )
  );

  private static final List<Throughput> THROUGHPUT = List.of(
      new Throughput(DATE_IN1, 100),
      new Throughput(DATE_IN2, 400),
      new Throughput(DATE_IN3, 500)
  );

  private static final List<ProcessPathRequest> CURRENT_BACKLOG = List.of(
      new ProcessPathRequest(
          TOT_MONO,
          List.of(
              new Quantity(null, DATE_OUT1, 100),
              new Quantity(null, DATE_OUT2, 200),
              new Quantity(null, DATE_OUT3, 300)
          )
      ),
      new ProcessPathRequest(
          NON_TOT_MONO,
          List.of(
              new Quantity(null, DATE_OUT1, 400),
              new Quantity(null, DATE_OUT2, 500),
              new Quantity(null, DATE_OUT3, 600)
          )
      )
  );

  private static final BacklogProjectionTotalRequest REQUEST = new BacklogProjectionTotalRequest(
      DATE_FROM,
      DATE_TO,
      new BacklogRequest(CURRENT_BACKLOG),
      new BacklogRequest(FORECASTED_BACKLOG),
      THROUGHPUT
  );

  private static final Map<Instant, Map<Instant, Map<ProcessPath, Long>>> EXPECTED = Map.of(
      //operation hour
      DATE_OUT1,
      Map.of(
          DATE_OUT1, Map.of(TOT_MONO, 120L, NON_TOT_MONO, 341L),
          DATE_OUT2, Map.of(TOT_MONO, 200L, NON_TOT_MONO, 500L),
          DATE_OUT3, Map.of(TOT_MONO, 300L, NON_TOT_MONO, 600L)
      ),
      //operation hour
      DATE_OUT2,
      Map.of(
          DATE_OUT1, Map.of(TOT_MONO, 0L, NON_TOT_MONO, 68L),
          DATE_OUT2, Map.of(TOT_MONO, 249L, NON_TOT_MONO, 520L),
          DATE_OUT3, Map.of(TOT_MONO, 300L, NON_TOT_MONO, 600L)
      ),
      //operation hour
      DATE_OUT3,
      Map.of(
          DATE_OUT1, Map.of(NON_TOT_MONO, 0L),
          DATE_OUT2, Map.of(TOT_MONO, 83L, NON_TOT_MONO, 255L),
          DATE_OUT3, Map.of(TOT_MONO, 360L, NON_TOT_MONO, 630L)
      )
  );

  private static final int INTERVAL_SIZE_IN_MINUTES = 60;

  @InjectMocks
  private BacklogUnifiedProjection backlogUnifiedProjection;

  @Test
  void testGetProjection() {

    final var res = backlogUnifiedProjection.getProjection(REQUEST, INTERVAL_SIZE_IN_MINUTES);

    assertion(res);
  }

  private void assertion(final Map<Instant, Map<Instant, Map<ProcessPath, Long>>> response) {
    assertEquals(EXPECTED.size(), response.size());

    EXPECTED.forEach((key, value) -> {
      assertTrue(response.containsKey(key));
      assertionBacklogByDateOut(value, response.get(key));
    });
  }

  private void assertionBacklogByDateOut(
      final Map<Instant, Map<ProcessPath, Long>> expected,
      final Map<Instant, Map<ProcessPath, Long>> response) {
    assertEquals(expected.size(), response.size());

    expected.forEach((key, value) -> {
      assertTrue(response.containsKey(key));
      assertionBacklogByProcessPath(value, response.get(key));
    });
  }

  private void assertionBacklogByProcessPath(
      final Map<ProcessPath, Long> expected,
      final Map<ProcessPath, Long> response) {
    assertEquals(expected.size(), response.size());

    expected.forEach((key, value) -> {
      assertTrue(response.containsKey(key));
      assertEquals(value, response.get(key));
    });
  }

}
