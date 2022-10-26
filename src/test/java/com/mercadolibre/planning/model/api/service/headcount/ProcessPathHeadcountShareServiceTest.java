package com.mercadolibre.planning.model.api.service.headcount;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.service.headcount.ProcessPathHeadcountShareService;
import com.mercadolibre.planning.model.api.domain.service.headcount.ProcessPathHeadcountShareService.ProcessPathShareGateway;
import com.mercadolibre.planning.model.api.domain.service.headcount.ProcessPathHeadcountShareService.ShareAtProcessPathAndProcessAndDate;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.util.TestUtils;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessPathHeadcountShareServiceTest {

  private static final List<Long> FORECAST_IDS = List.of(1L, 2L);

  private static final Instant DATE_FROM = TestUtils.A_DATE_UTC.toInstant();

  private static final Instant DATE_1 = DATE_FROM.plus(1L, HOURS);

  private static final Instant DATE_2 = DATE_FROM.plus(2L, HOURS);

  private static final Instant DATE_TO = DATE_FROM.plus(3L, HOURS);

  @InjectMocks
  ProcessPathHeadcountShareService processPathHeadcountShareService;

  @Mock
  GetForecastUseCase getForecastUseCase;

  @Mock
  ProcessPathShareGateway processPathShareGateway;

  @Test
  void testReturnsCalculatedShares() {
    // GIVE
    when(getForecastUseCase.execute(forecastInput()))
        .thenReturn(FORECAST_IDS);

    when(processPathShareGateway.getProcessPathHeadcountShare(
        Set.of(PICKING),
        DATE_FROM,
        DATE_TO,
        FORECAST_IDS
    )).thenReturn(mockedShares());

    // WHEN
    final var result = processPathHeadcountShareService.getHeadcountShareByProcessPath(
        LOGISTIC_CENTER_ID,
        FBM_WMS_OUTBOUND,
        Set.of(PICKING),
        DATE_FROM,
        DATE_TO,
        DATE_FROM
    );

    // THEN
    assertNotNull(result);
    assertEquals(expectedShares(), result);
  }

  private static GetForecastInput forecastInput() {
    return new GetForecastInput(
        LOGISTIC_CENTER_ID,
        FBM_WMS_OUTBOUND,
        ZonedDateTime.ofInstant(DATE_FROM, UTC),
        ZonedDateTime.ofInstant(DATE_TO, UTC),
        DATE_FROM
    );
  }

  private static List<ShareAtProcessPathAndProcessAndDate> mockedShares() {
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

  private static Map<ProcessPath, Map<ProcessName, Map<Instant, Double>>> expectedShares() {
    return Map.of(
        GLOBAL, Map.of(
            PICKING, Map.of(
                DATE_FROM, 1.0, DATE_1, 1.0, DATE_2, 1.0, DATE_TO, 1.0
            )
        ),
        TOT_MONO, Map.of(
            PICKING, Map.of(
                DATE_FROM, 0.5, DATE_1, 0.75
            )
        ),
        NON_TOT_MONO, Map.of(
            PICKING, Map.of(
                DATE_FROM, 0.5
            )
        ),
        TOT_MULTI_BATCH, Map.of(
            PICKING, Map.of(
                DATE_1, 0.25
            )
        ),
        TOT_MULTI_ORDER, Map.of(
            PICKING, Map.of(
                DATE_2, 0.34
            )
        ),
        NON_TOT_MULTI_ORDER, Map.of(
            PICKING, Map.of(
                DATE_2, 0.66
            )
        )
    );
  }
}
