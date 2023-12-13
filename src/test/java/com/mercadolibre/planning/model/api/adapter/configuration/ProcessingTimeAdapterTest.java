package com.mercadolibre.planning.model.api.adapter.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.adapter.configuration.ProcessingTimeAdapter.EtdProcessingTimeData;
import com.mercadolibre.planning.model.api.client.db.repository.configuration.OutboundProcessingTimeRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.OutboundProcessingTime;
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
class ProcessingTimeAdapterTest {

  private static final String LOGISTIC_CENTER_ID = "ARTW01";

  private static final String SATURDAY = "saturday";

  private static final String MONDAY = "monday";

  private static final String TUESDAY = "tuesday";

  private static final String WEDNESDAY = "wednesday";

  private static final String CPT_03 = "0300";

  private static final String CPT_05 = "0500";

  private static final String CPT_13 = "1300";

  private static final String CPT_21 = "2100";

  private static final String CPT_10 = "1000";

  private static final int PROCESSING_TIME_240 = 240;

  private static final Instant DATE_FROM_THURSDAY = Instant.parse("2023-11-16T00:00:00Z");

  private static final Instant DATE_TO_MONDAY = Instant.parse("2023-11-20T10:00:00Z");

  private static final ZoneId ZONE_ID = ZoneId.of("America/Bogota");


  private static final List<OutboundProcessingTime> OUTBOUND_PROCESSING_TIMES = List.of(
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, WEDNESDAY, CPT_21, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, SATURDAY, CPT_13, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, SATURDAY, CPT_21, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_05, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_03, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_10, PROCESSING_TIME_240, true)
  );

  private static final List<EtdProcessingTimeData> EXPECTED_OUTBOUND_PROCESSING_TIMES = List.of(
      new EtdProcessingTimeData(LOGISTIC_CENTER_ID, MONDAY, CPT_03, PROCESSING_TIME_240),
      new EtdProcessingTimeData(LOGISTIC_CENTER_ID, MONDAY, CPT_05, PROCESSING_TIME_240),
      new EtdProcessingTimeData(LOGISTIC_CENTER_ID, WEDNESDAY, CPT_21, PROCESSING_TIME_240),
      new EtdProcessingTimeData(LOGISTIC_CENTER_ID, SATURDAY, CPT_13, PROCESSING_TIME_240),
      new EtdProcessingTimeData(LOGISTIC_CENTER_ID, SATURDAY, CPT_21, PROCESSING_TIME_240)
  );

  @InjectMocks
  private ProcessingTimeAdapter processingTimeAdapter;

  @Mock
  private OutboundProcessingTimeRepository outboundProcessingTimeRepository;

  private static Stream<Arguments> provideArgumentsAndExpectedProcessingTimes() {
    return Stream.of(
        Arguments.of(OUTBOUND_PROCESSING_TIMES, EXPECTED_OUTBOUND_PROCESSING_TIMES),
        Arguments.of(List.of(), List.of())
    );
  }


  @ParameterizedTest
  @MethodSource("provideArgumentsAndExpectedProcessingTimes")
  void testGetOutboundProcessingTimeByCpt(
      final List<OutboundProcessingTime> processingTimes,
      final List<EtdProcessingTimeData> expectedProcessingTimes
  ) {

    // GIVEN
    when(outboundProcessingTimeRepository.findByLogisticCenterAndIsActive(LOGISTIC_CENTER_ID))
        .thenReturn(processingTimes);

    // WHEN
    final List<EtdProcessingTimeData> result = processingTimeAdapter.getOutboundProcessingTimeByCptInRange(
        LOGISTIC_CENTER_ID,
        DATE_FROM_THURSDAY,
        DATE_TO_MONDAY,
        ZONE_ID
    );

    // THEN
    assertEquals(expectedProcessingTimes.size(), result.size());
    assertEquals(expectedProcessingTimes, result);
    verify(outboundProcessingTimeRepository, times(1)).findByLogisticCenterAndIsActive(LOGISTIC_CENTER_ID);

  }
}
