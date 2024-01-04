package com.mercadolibre.planning.model.api.domain.service.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.service.configuration.ProcessingTimeService.FuryConfigServiceGateway;
import com.mercadolibre.planning.model.api.domain.service.configuration.ProcessingTimeService.OutboundProcessingTimeGateway;
import com.mercadolibre.planning.model.api.domain.service.configuration.SlaProcessingTimes.SlaProperties;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessingTimeServiceTest {

  private static final String LOGISTIC_CENTER_ID = "ARTW01";

  private static final String WEDNESDAY = "wednesday";

  private static final String MONDAY = "monday";

  private static final String TUESDAY = "tuesday";

  private static final String CPT_13 = "1300";

  private static final String CPT_21 = "2100";

  private static final String CPT_18 = "1830";

  private static final String CPT_10 = "1000";

  private static final ZoneId ZONE_ID = ZoneId.of("America/Buenos_Aires");

  private static final int PROCESSING_TIME_240 = 240;

  private static final int PROCESSING_TIME_165 = 165;

  private static final Instant DATE_FROM = Instant.parse("2023-12-11T15:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2023-12-11T23:59:00Z");

  private static final Instant DATE_TO_PLUS_DAYS = Instant.parse("2023-12-14T23:59:00Z");

  private static final Instant SLA_DATE_11_1600 = Instant.parse("2023-12-11T16:00:00Z");

  private static final Instant SLA_DATE_11_2130 = Instant.parse("2023-12-11T21:30:00Z");

  private static final Instant SLA_DATE_12_1300 = Instant.parse("2023-12-12T13:00:00Z");

  private static final Instant SLA_DATE_14_0000 = Instant.parse("2023-12-14T00:00:00Z");

  private static final List<DayAndHourProcessingTime> OUTBOUND_PROCESSING_TIMES_3_DAYS = List.of(
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, WEDNESDAY, CPT_21, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_18, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_13, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_10, PROCESSING_TIME_240)
  );

  private static final SlaProcessingTimes EMPTY_SLA_PROCESSING_TIMES = new SlaProcessingTimes(PROCESSING_TIME_240, List.of());

  private static final List<SlaProperties> RESPONSE_PROPERTIES_SAME_DAY = List.of(
      new SlaProperties(
          SLA_DATE_11_1600,
          PROCESSING_TIME_165
      ),
      new SlaProperties(
          SLA_DATE_11_2130,
          PROCESSING_TIME_240
      )
  );

  private static final SlaProcessingTimes SLA_PROCESSING_TIMES_240 = new SlaProcessingTimes(
      PROCESSING_TIME_240,
      RESPONSE_PROPERTIES_SAME_DAY
  );

  private static final List<SlaProperties> RESPONSE_PROPERTIES_3_DAYS = List.of(
      new SlaProperties(
          SLA_DATE_11_1600,
          PROCESSING_TIME_240
      ),
      new SlaProperties(
          SLA_DATE_11_2130,
          PROCESSING_TIME_240
      ),
      new SlaProperties(
          SLA_DATE_12_1300,
          PROCESSING_TIME_240
      ),
      new SlaProperties(
          SLA_DATE_14_0000,
          PROCESSING_TIME_240
      )
  );

  private static final SlaProcessingTimes SLA_PROCESSING_TIMES_165 = new SlaProcessingTimes(
      PROCESSING_TIME_165,
      RESPONSE_PROPERTIES_3_DAYS
  );

  @Mock
  private OutboundProcessingTimeGateway outboundProcessingTimeGateway;

  @Mock
  private FuryConfigServiceGateway furyConfigServiceGateway;

  @InjectMocks
  private ProcessingTimeService processingTimeService;

  private static Stream<Arguments> getProcessingTimeForCptInDateRangeProvider() {
    return Stream.of(
        Arguments.of(
            DATE_FROM,
            DATE_TO,
            RESPONSE_PROPERTIES_SAME_DAY.stream(),
            PROCESSING_TIME_240,
            SLA_PROCESSING_TIMES_240
        ),
        Arguments.of(
            DATE_FROM,
            DATE_TO_PLUS_DAYS,
            RESPONSE_PROPERTIES_3_DAYS.stream(),
            PROCESSING_TIME_165,
            SLA_PROCESSING_TIMES_165
        ),
        Arguments.of(DATE_FROM, DATE_TO, Stream.of(), PROCESSING_TIME_240, EMPTY_SLA_PROCESSING_TIMES)
    );
  }

  private static Stream<Arguments> updateProcessingTimeForCptsByLogisticCenterProvider() {
    return Stream.of(
        Arguments.of(OUTBOUND_PROCESSING_TIMES_3_DAYS),
        Arguments.of(List.of())
    );
  }

  @ParameterizedTest
  @MethodSource("getProcessingTimeForCptInDateRangeProvider")
  void testGetProcessingTimeForCptInDateRange(
      final Instant dateFrom,
      final Instant dateTo,
      final Stream<SlaProperties> outboundProcessingTimes,
      final int furyConfigDefaultValue,
      final SlaProcessingTimes expectedSlaProcessingTimes
  ) {
    // GIVEN
    when(outboundProcessingTimeGateway.getOutboundProcessingTimeByCptInRange(LOGISTIC_CENTER_ID, dateFrom, dateTo, ZONE_ID))
        .thenReturn(outboundProcessingTimes);

    when(furyConfigServiceGateway.getProcessingTime(LOGISTIC_CENTER_ID))
        .thenReturn(furyConfigDefaultValue);

    // WHEN
    SlaProcessingTimes response =
        processingTimeService.getProcessingTimeForCptInDateRange(LOGISTIC_CENTER_ID, dateFrom, dateTo, ZONE_ID);

    // THEN
    assertEquals(expectedSlaProcessingTimes, response);
  }

  @ParameterizedTest
  @MethodSource("updateProcessingTimeForCptsByLogisticCenterProvider")
  void testUpdateProcessingTimeForCptsByLogisticCenter(
      final List<DayAndHourProcessingTime> clientResponse
  ) {
    // GIVEN
    when(outboundProcessingTimeGateway.getOutboundProcessingTimeByLogisticCenterFromRouteClient(LOGISTIC_CENTER_ID))
        .thenReturn(clientResponse);

    when(outboundProcessingTimeGateway.updateOutboundProcessingTimesForLogisticCenter(anyString(), anyList()))
        .thenReturn(clientResponse);

    // WHEN
    List<DayAndHourProcessingTime> result =
        processingTimeService.updateProcessingTimeForCptsByLogisticCenter(LOGISTIC_CENTER_ID);

    // THEN
    verify(outboundProcessingTimeGateway, times(1))
        .updateOutboundProcessingTimesForLogisticCenter(LOGISTIC_CENTER_ID, clientResponse);
    assertEquals(clientResponse, result);
  }
}
