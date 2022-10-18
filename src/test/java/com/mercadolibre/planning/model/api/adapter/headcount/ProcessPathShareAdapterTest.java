package com.mercadolibre.planning.model.api.adapter.headcount;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.adapter.headcount.ProcessPathShareAdapter.ShareView;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.service.headcount.ProcessPathHeadcountShareService.ShareAtProcessPathAndProcessAndDate;
import com.mercadolibre.planning.model.api.util.TestUtils;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessPathShareAdapterTest {
  private static final List<Long> FORECAST_IDS = List.of(1L, 2L);

  private static final Instant DATE_FROM = TestUtils.A_DATE_UTC.toInstant();

  private static final Instant DATE_1 = DATE_FROM.plus(1L, HOURS);

  private static final Instant DATE_2 = DATE_FROM.plus(2L, HOURS);

  private static final Instant DATE_TO = DATE_FROM.plus(3L, HOURS);

  @Mock
  ProcessingDistributionRepository repository;

  @InjectMocks
  private ProcessPathShareAdapter adapter;

  private static ShareView view(
      final ProcessPath processPath,
      final ProcessName processName,
      final Instant date,
      final Double share
  ) {
    final var mock = Mockito.mock(ShareView.class);
    when(mock.getProcessPath()).thenReturn(processPath);
    when(mock.getProcessName()).thenReturn(processName);
    when(mock.getDate()).thenReturn(date);
    when(mock.getShare()).thenReturn(share);
    return mock;
  }

  private static List<ShareView> mockedViews() {
    return List.of(
        view(GLOBAL, PICKING, DATE_FROM, 1.0),
        view(TOT_MONO, PICKING, DATE_FROM, 0.5),
        view(NON_TOT_MONO, PICKING, DATE_FROM, 0.5),
        view(GLOBAL, PICKING, DATE_1, 1.0),
        view(TOT_MONO, PICKING, DATE_1, 0.75),
        view(TOT_MULTI_BATCH, PICKING, DATE_1, 0.25),
        view(GLOBAL, PICKING, DATE_2, 1.0),
        view(TOT_MULTI_ORDER, PICKING, DATE_2, 0.34),
        view(NON_TOT_MULTI_ORDER, PICKING, DATE_2, 0.66),
        view(GLOBAL, PICKING, DATE_TO, 1.0)
    );
  }

  private static List<ShareAtProcessPathAndProcessAndDate> expected() {
    return List.of(
        new ShareAtProcessPathAndProcessAndDate(GLOBAL, PICKING, DATE_FROM, 1.0),
        new ShareAtProcessPathAndProcessAndDate(TOT_MONO, PICKING, DATE_FROM, 0.5),
        new ShareAtProcessPathAndProcessAndDate(NON_TOT_MONO, PICKING, DATE_FROM, 0.5),
        new ShareAtProcessPathAndProcessAndDate(GLOBAL, PICKING, DATE_1, 1.0),
        new ShareAtProcessPathAndProcessAndDate(TOT_MONO, PICKING, DATE_1, 0.75),
        new ShareAtProcessPathAndProcessAndDate(TOT_MULTI_BATCH, PICKING, DATE_1, 0.25),
        new ShareAtProcessPathAndProcessAndDate(GLOBAL, PICKING, DATE_2, 1.0),
        new ShareAtProcessPathAndProcessAndDate(TOT_MULTI_ORDER, PICKING, DATE_2, 0.34),
        new ShareAtProcessPathAndProcessAndDate(NON_TOT_MULTI_ORDER, PICKING, DATE_2, 0.66),
        new ShareAtProcessPathAndProcessAndDate(GLOBAL, PICKING, DATE_TO, 1.0)
    );
  }

  @Test
  void testRetrieveSharesOk() {
    // GIVEN
    final var views = mockedViews();
    when(repository.getProcessPathHeadcountShare(List.of("picking"), DATE_FROM, DATE_TO, FORECAST_IDS))
        .thenReturn(views);

    // WHEN
    final var result = adapter.getProcessPathHeadcountShare(
        Set.of(PICKING),
        DATE_FROM,
        DATE_TO,
        FORECAST_IDS
    );

    // THEN
    assertNotNull(result);
    assertEquals(expected(), result);
  }

  @Test
  void testRetrieveSharesThrowsException() {
    // GIVEN
    when(repository.getProcessPathHeadcountShare(List.of("picking"), DATE_FROM, DATE_TO, FORECAST_IDS))
        .thenThrow(RuntimeException.class);

    // WHEN
    assertThrows(RuntimeException.class, () -> adapter.getProcessPathHeadcountShare(
        Set.of(PICKING),
        DATE_FROM,
        DATE_TO,
        FORECAST_IDS
    ));
  }
}
