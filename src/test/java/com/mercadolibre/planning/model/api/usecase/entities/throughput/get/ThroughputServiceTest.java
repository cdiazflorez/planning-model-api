package com.mercadolibre.planning.model.api.usecase.entities.throughput.get;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.ThroughputService;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThroughputServiceTest {

  private static final double ACCEPTED_DELTA = 0.001;

  private static final ZonedDateTime DATE_FROM = ZonedDateTime.parse("2023-01-01T12:00:00Z");

  private static final ZonedDateTime DATE_TO = ZonedDateTime.parse("2023-01-01T13:00:00Z");

  private static final List<EntityOutput> THROUGHPUTS = List.of(
      entity(TOT_MONO, PICKING, DATE_FROM, 10D),
      entity(TOT_MONO, PICKING, DATE_TO, 20D),
      entity(TOT_MONO, PACKING, DATE_FROM, 0D),
      entity(TOT_MONO, PACKING, DATE_TO, 0D),
      entity(TOT_MULTI_ORDER, PICKING, DATE_FROM, 30D),
      entity(TOT_MULTI_ORDER, PICKING, DATE_TO, 40D),
      entity(TOT_MULTI_ORDER, PACKING, DATE_FROM, 0D),
      entity(TOT_MULTI_ORDER, PACKING, DATE_TO, 0D)
  );


  @InjectMocks
  private ThroughputService service;

  @Mock
  private GetThroughputUseCase useCase;

  private static EntityOutput entity(final ProcessPath path, final ProcessName process, final ZonedDateTime date, final Double quantity) {
    return EntityOutput.builder()
        .processPath(path)
        .processName(process)
        .date(date)
        .quantity(quantity)
        .build();
  }

  @Test
  void testGetThroughputRatio() {
    // GIVEN
    final var processes = Set.of(PICKING, PACKING);
    final var paths = List.of(TOT_MONO, TOT_MULTI_ORDER);

    when(useCase.execute(any(GetEntityInput.class))).thenReturn(THROUGHPUTS);

    // WHEN
    final var results = service.getThroughputRatioByProcessPath(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND, paths, processes, DATE_FROM.toInstant(), DATE_TO.toInstant(), DATE_FROM.toInstant()
    );

    // THEN
    final var totMono = results.get(TOT_MONO);
    assertEquals(0.2500, totMono.get(PICKING).get(DATE_FROM.toInstant()), ACCEPTED_DELTA);
    assertEquals(0.3333, totMono.get(PICKING).get(DATE_TO.toInstant()), ACCEPTED_DELTA);
    assertEquals(0D, totMono.get(PACKING).get(DATE_FROM.toInstant()), ACCEPTED_DELTA);
    assertEquals(0D, totMono.get(PACKING).get(DATE_TO.toInstant()), ACCEPTED_DELTA);

    final var totMultiOrder = results.get(TOT_MULTI_ORDER);
    assertEquals(0.7500, totMultiOrder.get(PICKING).get(DATE_FROM.toInstant()), ACCEPTED_DELTA);
    assertEquals(0.6666, totMultiOrder.get(PICKING).get(DATE_TO.toInstant()), ACCEPTED_DELTA);
    assertEquals(0D, totMultiOrder.get(PACKING).get(DATE_FROM.toInstant()), ACCEPTED_DELTA);
    assertEquals(0D, totMultiOrder.get(PACKING).get(DATE_TO.toInstant()), ACCEPTED_DELTA);
  }
}
