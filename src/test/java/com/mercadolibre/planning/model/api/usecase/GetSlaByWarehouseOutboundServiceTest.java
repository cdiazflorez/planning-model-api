package com.mercadolibre.planning.model.api.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.sla.Canalization;
import com.mercadolibre.planning.model.api.domain.entity.sla.CarrierServiceId;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.entity.sla.RouteCoverageResult;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.DayDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.gateway.RouteCoverageClientGateway;
import com.mercadolibre.planning.model.api.gateway.RouteEtsGateway;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("PMD.LongVariable")
@ExtendWith(MockitoExtension.class)
public class GetSlaByWarehouseOutboundServiceTest {

  private static final String LOGISTIC_CENTER_ID = "ARBA01";

  private static final String LOGISTIC_CENTER_ID_NOT_ARBA = "ARTW01";

  private static final String TIME_ZONE = "America/Argentina/Buenos_Aires";

  private static final String ACTIVE = "active";

  private static final String INACTIVE = "inactive";

  private static final String SATURDAY = "saturday";

  private static final String MONDAY = "monday";

  private static final String TUESDAY = "tuesday";

  private static final String SERVICE_ID_831 = "831";

  private static final String CPT_08 = "0800";

  private static final String PROCESSING_TIME_0400 = "0400";

  private static final String CPT = "cpt";

  private static final String ROUTE_ID = "ARBA01_I_931";

  private static final String CANALIZATION_X = "X";

  private static final ZonedDateTime UTC_DAY = ZonedDateTime.parse("2021-11-02T00:00:00.000000-00:00" + "[UTC]");

  private static final ZonedDateTime UTC_DAY_2 = ZonedDateTime.parse("2021-11-03T00:00:00.000000-00:00" + "[UTC]");

  private static final Date DATE_1 = new Date(2020, 1, 20);

  private static final Date DATE_2 = new Date(2021, 1, 21);

  private static final Map<String, List<DayDto>> FIXED_ETS_BY_DAY = Map.of(
      MONDAY,
      List.of(
          new DayDto(MONDAY, "2100", PROCESSING_TIME_0400, CPT, false),
          new DayDto(MONDAY, "2200", "0200", "ets", false)),
      TUESDAY,
      List.of(
          new DayDto(TUESDAY, CPT_08, "0600", CPT, false),
          new DayDto(TUESDAY, CPT_08, PROCESSING_TIME_0400, CPT, false),
          new DayDto(TUESDAY, CPT_08, "0500", CPT, false)),
      SATURDAY,
      List.of(new DayDto(SATURDAY, CPT_08, PROCESSING_TIME_0400, CPT, false)));

  @InjectMocks
  private GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  @Mock
  private RouteEtsGateway routeEtsGateway;

  @Mock
  private RouteCoverageClientGateway routeCoverageClientGateway;

  @ParameterizedTest
  @MethodSource("obtainSlaByZonedDataSupplier")
  public void obtainSlaByZonedDate(String logisticCenterId, List<GetSlaByWarehouseOutput> expected) {
    // GIVEN
    final ClientException exception = mock(ClientException.class);
    when(exception.getSuppressed()).thenReturn(new Throwable[0]);
    when(exception.getMessage()).thenReturn("exception");

    when(routeEtsGateway.postRoutEts(
        RouteEtsRequest.builder().fromFilter(List.of(logisticCenterId)).build()))
        .thenThrow(exception);

    final GetSlaByWarehouseInput input =
        new GetSlaByWarehouseInput(logisticCenterId, UTC_DAY, UTC_DAY.plusDays(1),
            List.of(UTC_DAY, UTC_DAY), TIME_ZONE);

    // WHEN
    final List<GetSlaByWarehouseOutput> actual = getSlaByWarehouseOutboundService.execute(input);

    // THEN
    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i).getDate(), actual.get(i).getDate());
      assertEquals(expected.get(i).getProcessingTime(), actual.get(i).getProcessingTime());
    }
  }

  @Test
  public void obtainCpt() {
    // GIVEN
    when(routeEtsGateway.postRoutEts(
        RouteEtsRequest.builder().fromFilter(List.of(LOGISTIC_CENTER_ID)).build()))
        .thenReturn(mockResponse());

    when(routeCoverageClientGateway.get(LOGISTIC_CENTER_ID))
        .thenReturn(mockResponseCoverageWithResult());

    final List<ZonedDateTime> backlog = List.of(UTC_DAY_2, UTC_DAY, UTC_DAY.plusHours(17));

    final GetSlaByWarehouseInput input =
        new GetSlaByWarehouseInput(LOGISTIC_CENTER_ID, UTC_DAY, UTC_DAY.plusDays(1), backlog, TIME_ZONE);

    // WHEN
    final List<GetSlaByWarehouseOutput> actual = getSlaByWarehouseOutboundService.execute(input);

    // THEN
    final List<GetSlaByWarehouseOutput> expected = mockCptOutput();

    assertEquals(expected.size(), actual.size());

    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i).getDate(), actual.get(i).getDate());
      assertEquals(expected.get(i).getProcessingTime(), actual.get(i).getProcessingTime());
    }
  }

  private List<GetSlaByWarehouseOutput> mockCptOutput() {
    final GetSlaByWarehouseOutput getSlaByWarehouseOutput =
        GetSlaByWarehouseOutput.builder()
            .logisticCenterId(LOGISTIC_CENTER_ID)
            .date(UTC_DAY)
            .processingTime(new ProcessingTime(240, MetricUnit.MINUTES))
            .build();

    final GetSlaByWarehouseOutput getSlaByWarehouseOutput2 =
        GetSlaByWarehouseOutput.builder()
            .logisticCenterId(LOGISTIC_CENTER_ID)
            .date(UTC_DAY.plusHours(1))
            .processingTime(new ProcessingTime(120, MetricUnit.MINUTES))
            .build();

    final GetSlaByWarehouseOutput getSlaByWarehouseOutput3 =
        GetSlaByWarehouseOutput.builder()
            .logisticCenterId(LOGISTIC_CENTER_ID)
            .date(UTC_DAY.plusHours(11))
            .processingTime(new ProcessingTime(240, MetricUnit.MINUTES))
            .build();

    final GetSlaByWarehouseOutput getSlaByWarehouseOutput4 =
        GetSlaByWarehouseOutput.builder()
            .logisticCenterId(LOGISTIC_CENTER_ID)
            .date(UTC_DAY_2)
            .processingTime(new ProcessingTime(240, MetricUnit.MINUTES))
            .build();


    final GetSlaByWarehouseOutput getSlaByWarehouseOutput5 =
        GetSlaByWarehouseOutput.builder()
            .logisticCenterId(LOGISTIC_CENTER_ID)
            .date(UTC_DAY.plusHours(17))
            .processingTime(new ProcessingTime(60, MetricUnit.MINUTES))
            .build();

    return List.of(
        getSlaByWarehouseOutput,
        getSlaByWarehouseOutput2,
        getSlaByWarehouseOutput3,
        getSlaByWarehouseOutput5,
        getSlaByWarehouseOutput4);
  }

  private List<RouteEtsDto> mockResponse() {

    final RouteEtsDto routeEtsDto =
        new RouteEtsDto(
            "ARBA01_X_931",
            LOGISTIC_CENTER_ID,
            CANALIZATION_X,
            SERVICE_ID_831,
            FIXED_ETS_BY_DAY,
            DATE_1,
            DATE_2,
            ACTIVE);

    final RouteEtsDto routeEtsDtoBadCanalization =
        new RouteEtsDto(
            ROUTE_ID,
            LOGISTIC_CENTER_ID,
            CANALIZATION_X,
            SERVICE_ID_831,
            FIXED_ETS_BY_DAY,
            DATE_1,
            DATE_2,
            ACTIVE);

    final RouteEtsDto routeEtsDtoBadServiceId =
        new RouteEtsDto(
            ROUTE_ID,
            LOGISTIC_CENTER_ID,
            CANALIZATION_X,
            "8312",
            FIXED_ETS_BY_DAY,
            DATE_1,
            DATE_2,
            ACTIVE);

    return List.of(routeEtsDto, routeEtsDto,
        routeEtsDtoBadCanalization, routeEtsDtoBadServiceId);
  }


  private List<RouteCoverageResult> mockResponseCoverageWithResult() {

    return List.of(
        new RouteCoverageResult(
            new Canalization(CANALIZATION_X,
                List.of(
                    new CarrierServiceId(SERVICE_ID_831))),
            ACTIVE),
        new RouteCoverageResult(
            new Canalization(CANALIZATION_X,
                List.of(
                    new CarrierServiceId("123"))),
            ACTIVE),
        new RouteCoverageResult(
            new Canalization("12345",
                List.of(
                    new CarrierServiceId("158663"))),
            INACTIVE));

  }

  private static Stream<Arguments> obtainSlaByZonedDataSupplier() {
    return Stream.of(
        Arguments.of(LOGISTIC_CENTER_ID, mockCptOutputByZonedDate()),
        Arguments.of(LOGISTIC_CENTER_ID_NOT_ARBA, mockCptOutputByZonedDate())
    );
  }

  private static List<GetSlaByWarehouseOutput> mockCptOutputByZonedDate() {

    final GetSlaByWarehouseOutput getSlaByWarehouseOutput =
        GetSlaByWarehouseOutput.builder()
            .serviceId(null)
            .canalizationId(null)
            .logisticCenterId(LOGISTIC_CENTER_ID)
            .date(UTC_DAY)
            .processingTime(new ProcessingTime(240, MetricUnit.MINUTES))
            .build();

    return List.of(getSlaByWarehouseOutput);
  }

}
