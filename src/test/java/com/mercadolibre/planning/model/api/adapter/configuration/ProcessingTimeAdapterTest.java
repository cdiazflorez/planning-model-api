package com.mercadolibre.planning.model.api.adapter.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.api.client.db.repository.configuration.OutboundProcessingTimeRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.OutboundProcessingTime;
import com.mercadolibre.planning.model.api.domain.service.configuration.DayAndHourProcessingTime;
import com.mercadolibre.planning.model.api.domain.service.configuration.SlaProcessingTimes.SlaProperties;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.DayDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;
import com.mercadolibre.planning.model.api.exception.ProcessingTimeException;
import com.mercadolibre.planning.model.api.gateway.RouteEtsGateway;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;

@ExtendWith(MockitoExtension.class)
class ProcessingTimeAdapterTest {

  private static final String LOGISTIC_CENTER_ID = "ARTW01";

  private static final String ACTIVE = "active";

  private static final String INACTIVE = "inactive";

  private static final String SATURDAY = "saturday";

  private static final String MONDAY = "monday";

  private static final String TUESDAY = "tuesday";

  private static final String WEDNESDAY = "wednesday";

  private static final String SUNDAY = "sunday";

  private static final String CPT = "cpt";

  private static final String CPT_03 = "0300";

  private static final String CPT_05 = "0500";

  private static final String CPT_13 = "1300";

  private static final String CPT_21 = "2100";

  private static final String CPT_08 = "0800";

  private static final String CPT_10 = "1000";

  private static final String PROCESSING_TIME_0400 = "0400";

  private static final String PROCESSING_TIME_0245 = "0245";

  private static final int PROCESSING_TIME_240 = 240;

  private static final Instant DATE_FROM_THURSDAY = Instant.parse("2023-11-16T00:00:00Z");

  private static final Instant DATE_FROM_MONDAY = Instant.parse("2023-11-20T06:30:00Z");

  private static final Instant DATE_TO_MONDAY = Instant.parse("2023-11-20T10:00:00Z");

  private static final Instant SLA_DATE_16_0000 = Instant.parse("2023-11-16T00:00:00Z");

  private static final Instant SLA_DATE_18_1600 = Instant.parse("2023-11-18T16:00:00Z");

  private static final Instant SLA_DATE_19_0000 = Instant.parse("2023-11-19T00:00:00Z");

  private static final Instant SLA_DATE_20_0600 = Instant.parse("2023-11-20T06:00:00Z");

  private static final Instant SLA_DATE_20_0800 = Instant.parse("2023-11-20T08:00:00Z");


  private static final ZoneId ZONE_ID = ZoneId.of("America/Buenos_Aires");

  private static final Date DATE_1 = new Date(2021, Calendar.OCTOBER, 21);

  private static final Date DATE_2 = new Date(2023, Calendar.NOVEMBER, 20);

  private static final RouteEtsRequest ROUTE_ETS_REQUEST = RouteEtsRequest.builder().fromFilter(List.of(LOGISTIC_CENTER_ID)).build();

  private static final List<OutboundProcessingTime> OUTBOUND_PROCESSING_TIMES = List.of(
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, WEDNESDAY, CPT_21, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, SATURDAY, CPT_13, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, SATURDAY, CPT_21, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_05, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_03, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_10, PROCESSING_TIME_240, true)
  );

  private static final List<OutboundProcessingTime> OUTBOUND_PROCESSING_TIMES_ROUTES = List.of(
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_13, 165, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_21, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_08, 300, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_10, PROCESSING_TIME_240, true),
      new OutboundProcessingTime(LOGISTIC_CENTER_ID, SATURDAY, CPT_21, PROCESSING_TIME_240, true)
  );

  private static final Stream<SlaProperties> EXPECTED_SLA_PROPERTIES = Stream.of(
      new SlaProperties(SLA_DATE_16_0000, PROCESSING_TIME_240),
      new SlaProperties(SLA_DATE_18_1600, PROCESSING_TIME_240),
      new SlaProperties(SLA_DATE_19_0000, PROCESSING_TIME_240),
      new SlaProperties(SLA_DATE_20_0600, PROCESSING_TIME_240),
      new SlaProperties(SLA_DATE_20_0800, PROCESSING_TIME_240)
  );

  private static final Stream<SlaProperties> EXPECTED_SAME_DAY_SLA_PROPERTIES = Stream.of(
      new SlaProperties(SLA_DATE_20_0800, PROCESSING_TIME_240)
  );

  private static final List<DayAndHourProcessingTime> ETD_PROCESSING_TIME_DATA = List.of(
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_13, 165),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, MONDAY, CPT_21, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_08, 300),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, TUESDAY, CPT_10, PROCESSING_TIME_240),
      new DayAndHourProcessingTime(LOGISTIC_CENTER_ID, SATURDAY, CPT_21, PROCESSING_TIME_240)
  );

  private static final Map<String, List<DayDto>> FIXED_ETS_BY_DAY = Map.of(
      SUNDAY, List.of(),
      MONDAY, List.of(
          new DayDto(MONDAY, CPT_13, PROCESSING_TIME_0245, CPT, false),
          new DayDto(MONDAY, CPT_21, PROCESSING_TIME_0400, CPT, false)),
      TUESDAY, List.of(
          new DayDto(TUESDAY, CPT_08, "0600", CPT, false),
          new DayDto(TUESDAY, CPT_08, "0500", CPT, false),
          new DayDto(TUESDAY, CPT_10, PROCESSING_TIME_0400, CPT, false)),
      SATURDAY, List.of(
          new DayDto(SATURDAY, CPT_21, PROCESSING_TIME_0400, CPT, false)));

  private static final RouteEtsDto ROUTE_ETS_DTO =
      new RouteEtsDto(
          "ARTW01_X_931",
          LOGISTIC_CENTER_ID,
          "X",
          "831",
          FIXED_ETS_BY_DAY,
          DATE_1,
          DATE_2,
          ACTIVE
      );

  private static final RouteEtsDto ROUTE_ETS_DTO_INACTIVE =
      new RouteEtsDto(
          "ARTW01_X_932",
          LOGISTIC_CENTER_ID,
          "Y",
          "832",
          Map.of(
              SUNDAY, List.of(
                  new DayDto(SUNDAY, CPT_13, PROCESSING_TIME_0245, CPT, false),
                  new DayDto(SUNDAY, CPT_21, PROCESSING_TIME_0400, CPT, false))
          ),
          DATE_1,
          DATE_2,
          INACTIVE
      );

  private static final List<RouteEtsDto> ROUTE_ETS_CLIENT_RESPONSE = List.of(ROUTE_ETS_DTO, ROUTE_ETS_DTO_INACTIVE);

  @InjectMocks
  private ProcessingTimeAdapter processingTimeAdapter;

  @Mock
  private OutboundProcessingTimeRepository outboundProcessingTimeRepository;

  @Mock
  private RouteEtsGateway routeEtsGateway;

  private static Stream<Arguments> provideArgumentsAndExpectedProcessingTimes() {
    return Stream.of(
        Arguments.of(OUTBOUND_PROCESSING_TIMES, EXPECTED_SLA_PROPERTIES, DATE_FROM_THURSDAY, DATE_TO_MONDAY),
        Arguments.of(OUTBOUND_PROCESSING_TIMES, EXPECTED_SAME_DAY_SLA_PROPERTIES, DATE_FROM_MONDAY, DATE_TO_MONDAY),
        Arguments.of(List.of(), Stream.of(), DATE_FROM_MONDAY, DATE_TO_MONDAY)
    );
  }

  private static Stream<Arguments> provideArgumentsAndExpectedRouteProcessingTimes() {
    return Stream.of(
        Arguments.of(ROUTE_ETS_CLIENT_RESPONSE, ETD_PROCESSING_TIME_DATA),
        Arguments.of(List.of(), List.of())
    );
  }

  @ParameterizedTest
  @MethodSource("provideArgumentsAndExpectedProcessingTimes")
  void testGetOutboundProcessingTimeByCpt(
      final List<OutboundProcessingTime> processingTimes,
      final Stream<SlaProperties> expectedProcessingTimes,
      final Instant dateFrom,
      final Instant dateTo
  ) {

    // GIVEN
    when(outboundProcessingTimeRepository.findByLogisticCenterAndIsActive(LOGISTIC_CENTER_ID))
        .thenReturn(processingTimes);

    // WHEN
    final Stream<SlaProperties> result = processingTimeAdapter.getOutboundProcessingTimeByCptInRange(
        LOGISTIC_CENTER_ID,
        dateFrom,
        dateTo,
        ZONE_ID
    );

    // THEN
    assertEquals(expectedProcessingTimes.collect(Collectors.toSet()), result.collect(Collectors.toSet()));
    verify(outboundProcessingTimeRepository, times(1)).findByLogisticCenterAndIsActive(LOGISTIC_CENTER_ID);
  }

  @ParameterizedTest
  @MethodSource("provideArgumentsAndExpectedRouteProcessingTimes")
  void testGetOutboundProcessingTimeByLogisticCenterFromRouteClient(
      final List<RouteEtsDto> routeEtsProcessingTimes,
      final List<DayAndHourProcessingTime> expectedProcessingTimes
  ) {

    // GIVEN
    when(routeEtsGateway.postRoutEts(ROUTE_ETS_REQUEST))
        .thenReturn(routeEtsProcessingTimes);

    // WHEN
    final List<DayAndHourProcessingTime> result = processingTimeAdapter
        .getOutboundProcessingTimeByLogisticCenterFromRouteClient(LOGISTIC_CENTER_ID);

    // THEN
    assertEquals(expectedProcessingTimes.size(), result.size());
    assertEquals(expectedProcessingTimes, result);
  }

  @Test
  void testUpdateOutboundProcessingTimesForLogisticCenter() {
    // GIVEN
    when(outboundProcessingTimeRepository.saveAll(OUTBOUND_PROCESSING_TIMES_ROUTES))
        .thenReturn(OUTBOUND_PROCESSING_TIMES_ROUTES);

    // WHEN
    final List<DayAndHourProcessingTime> result = processingTimeAdapter
        .updateOutboundProcessingTimesForLogisticCenter(LOGISTIC_CENTER_ID, ETD_PROCESSING_TIME_DATA);

    // THEN
    verify(outboundProcessingTimeRepository, times(1)).purgeOldRecords(any());
    verify(outboundProcessingTimeRepository, times(1)).deactivateAllByLogisticCenter(LOGISTIC_CENTER_ID);
    verify(outboundProcessingTimeRepository, times(1)).saveAll(OUTBOUND_PROCESSING_TIMES_ROUTES);
    assertEquals(ETD_PROCESSING_TIME_DATA, result);
  }

  @Test
  void validateDataAccessExceptionInGetProcessingTime() {

    // GIVEN
    when(outboundProcessingTimeRepository.findByLogisticCenterAndIsActive(LOGISTIC_CENTER_ID))
        .thenThrow(EmptyResultDataAccessException.class);

    // WHEN - THEN
    assertThrows(
        ProcessingTimeException.class,
        () -> processingTimeAdapter.getOutboundProcessingTimeByCptInRange(
            LOGISTIC_CENTER_ID,
            DATE_FROM_THURSDAY,
            DATE_TO_MONDAY,
            ZONE_ID
        )
    );
  }

  @Test
  void validateClientExceptionInUpdateProcessingTime() {
    // GIVEN
    final ClientException clientException = mock(ClientException.class);

    when(clientException.getMessage()).thenReturn("API error");
    when(routeEtsGateway.postRoutEts(
        RouteEtsRequest.builder()
            .fromFilter(List.of(LOGISTIC_CENTER_ID))
            .build()
    )).thenThrow(clientException);

    // WHEN - THEN
    assertThrows(
        ProcessingTimeException.class,
        () -> processingTimeAdapter.getOutboundProcessingTimeByLogisticCenterFromRouteClient(LOGISTIC_CENTER_ID));
  }

  @Test
  void validateDataAccessExceptionInUpdateProcessingTime() throws ProcessingTimeException {

    // GIVEN
    when(outboundProcessingTimeRepository.saveAll(OUTBOUND_PROCESSING_TIMES_ROUTES))
        .thenThrow(EmptyResultDataAccessException.class);

    // WHEN - THEN
    assertThrows(
        ProcessingTimeException.class,
        () -> processingTimeAdapter.updateOutboundProcessingTimesForLogisticCenter(LOGISTIC_CENTER_ID, ETD_PROCESSING_TIME_DATA));
  }

}
