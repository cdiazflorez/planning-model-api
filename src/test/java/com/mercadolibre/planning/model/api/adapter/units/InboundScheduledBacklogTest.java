package com.mercadolibre.planning.model.api.adapter.units;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.Photo;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboundScheduledBacklogTest {
  private static final String DATE_IN_KEY = "date_in";
  private static final String DATE_OUT_KEY = "date_out";
  private static final String WORKFLOW_KEY = "workflow";
  private static final String PATH_KEY = "path";
  private static final String INBOUND_WORKFLOW = "inbound";
  private static final String INBOUND_TRANSFER_WORKFLOW = "inbound-transfer";

  @Mock
  private BacklogGateway backlogGateway;

  @InjectMocks
  private InboundScheduledBacklogAdapter backlogAdapter;

  @Test
  void testAdapterWithNullBacklog() {
    when(backlogGateway.getLastPhoto(any())).thenReturn(null);
    //When
    final var result = backlogAdapter.getScheduledBacklog(
        "SOME_WAREHOUSE",
        List.of(Workflow.FBM_WMS_INBOUND),
        Instant.now(),
        Instant.now(),
        Instant.now()
    );
    //then
    assertEquals(0, result.size());
  }

  @Test
  void testAdapterWithBacklog() {
    when(backlogGateway.getLastPhoto(any())).thenReturn(getPhotoToTest());
    //When
    final var result = backlogAdapter.getScheduledBacklog(
        "SOME_WAREHOUSE",
        List.of(Workflow.FBM_WMS_INBOUND),
        Instant.now(),
        Instant.now(),
        Instant.now()
    );

    //Then
    assertNotNull(result);
    assertEquals(2, result.size());
    final var first = result.get(0);
    assertEquals(INBOUND_TRANSFER_WORKFLOW, first.getWorkflow());
    assertEquals(Instant.parse("2020-01-01T14:00:00Z"), first.getDateIn());
    assertEquals(Instant.parse("2020-01-03T14:00:00Z"), first.getDateOut());
    assertNull(first.getPath());

    final var second = result.get(1);
    assertEquals(INBOUND_WORKFLOW, second.getWorkflow());
    assertEquals(Instant.parse("2020-01-01T17:00:00Z"), second.getDateIn());
    assertEquals(Instant.parse("2020-01-03T16:00:00Z"), second.getDateOut());
    assertEquals(Path.FTL, second.getPath());
  }

  private static Photo getPhotoToTest() {
    return new Photo(
        Instant.now(),
        of(
            createPhotoGroup(INBOUND_TRANSFER_WORKFLOW, "2020-01-01T14:00:00Z", "2020-01-03T14:00:00Z", null, 10, 20),
            createPhotoGroup(INBOUND_WORKFLOW, "2020-01-01T17:00:00Z", "2020-01-03T16:00:00Z", "FTL", 30, 50)
        )
    );
  }

  private static Photo.Group createPhotoGroup(
      final String workflow,
      final String dateIn,
      final String dateOut,
      final String path,
      int total,
      int accumulateTotal
  ) {
    final Map<String, String> key = new ConcurrentHashMap<>();
    key.put(DATE_IN_KEY, dateIn);
    key.put(DATE_OUT_KEY, dateOut);
    key.put(WORKFLOW_KEY, workflow);
    if (path != null) {
      key.put(PATH_KEY, path);
    }
    return new Photo.Group(key, total, accumulateTotal);
  }
}
