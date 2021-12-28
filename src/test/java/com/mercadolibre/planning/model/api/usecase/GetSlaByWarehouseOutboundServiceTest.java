package com.mercadolibre.planning.model.api.usecase;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.LongVariable")
@ExtendWith(MockitoExtension.class)
public class GetSlaByWarehouseOutboundServiceTest {
    private static final String TIME_ZONE = "America/Argentina/Buenos_Aires";

    private static final ZonedDateTime DAY =
            ZonedDateTime.parse(
                    "2021-11-02T00:00:00.000000-00:00" + "[UTC]"); // /LUNES 2021-11-01 A LAS 21

    private static final ZonedDateTime NOW = ZonedDateTime.now();

    @InjectMocks
    private GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

    @Mock
    private RouteEtsGateway routeEtsGateway;

    @Mock
    private RouteCoverageClientGateway routeCoverageClientGateway;


    @Test
    public void obtainSlaByZonedDate() {
        // GIVEN
        final ClientException exception = mock(ClientException.class);
        when(exception.getMessage()).thenReturn("exception");

        when(routeEtsGateway.postRoutEts(
                RouteEtsRequest.builder().fromFilter(List.of("ARBA01")).build()))
                .thenThrow(exception);

        final GetSlaByWarehouseInput input =
                new GetSlaByWarehouseInput("ARBA01", DAY, DAY.plusDays(1),
                        List.of(DAY, DAY), TIME_ZONE);

        // WHEN
        final List<GetSlaByWarehouseOutput> actual = getSlaByWarehouseOutboundService.execute(input);

        // THEN
        final List<GetSlaByWarehouseOutput> expected = mockCptOutputByZonedDate();

        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).getDate(), actual.get(i).getDate());
            assertEquals(expected.get(i).getProcessingTime(), actual.get(i).getProcessingTime());
        }
    }

    @Test
    public void obtainCpt() {
        // GIVEN
        when(routeEtsGateway.postRoutEts(
                RouteEtsRequest.builder().fromFilter(List.of("ARBA01")).build()))
                .thenReturn(mockResponse());

        when(routeCoverageClientGateway.get("ARBA01"))
                .thenReturn(mockResponseCoverageWithResult());

        final List<ZonedDateTime> backlog = List.of(NOW, DAY);

        final GetSlaByWarehouseInput input =
                new GetSlaByWarehouseInput("ARBA01", DAY, DAY.plusDays(1), backlog, TIME_ZONE);

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
                        .logisticCenterId("ARBA01")
                        .date(DAY)
                        .processingTime(new ProcessingTime(240, MetricUnit.MINUTES))
                        .build();

        final GetSlaByWarehouseOutput getSlaByWarehouseOutput2 =
                GetSlaByWarehouseOutput.builder()
                        .logisticCenterId("ARBA01")
                        .date(DAY.plusHours(1))
                        .processingTime(new ProcessingTime(120, MetricUnit.MINUTES))
                        .build();

        final GetSlaByWarehouseOutput getSlaByWarehouseOutput3 =
                GetSlaByWarehouseOutput.builder()
                        .logisticCenterId("ARBA01")
                        .date(DAY.plusHours(11))
                        .processingTime(new ProcessingTime(240, MetricUnit.MINUTES))
                        .build();

        final GetSlaByWarehouseOutput getSlaByWarehouseOutput4 =
                GetSlaByWarehouseOutput.builder()
                        .logisticCenterId("ARBA01")
                        .date(NOW)
                        .processingTime(new ProcessingTime(240, MetricUnit.MINUTES))
                        .build();

        return List.of(getSlaByWarehouseOutput, getSlaByWarehouseOutput2,
                getSlaByWarehouseOutput3, getSlaByWarehouseOutput4);
    }

    private List<GetSlaByWarehouseOutput> mockCptOutputByZonedDate() {

        final GetSlaByWarehouseOutput getSlaByWarehouseOutput =
                GetSlaByWarehouseOutput.builder()
                        .serviceId(null)
                        .canalizationId(null)
                        .logisticCenterId("ARBA01")
                        .date(DAY)
                        .processingTime(new ProcessingTime(240, MetricUnit.MINUTES))
                        .build();

        return List.of(getSlaByWarehouseOutput);
    }

    private List<RouteEtsDto> mockResponse() {

        final RouteEtsDto routeEtsDto =
                new RouteEtsDto(
                        "ARBA01_X_931",
                        "ARBA01",
                        "X",
                        "831",
                        Map.of(
                                "monday",
                                List.of(
                                        new DayDto("monday", "2100", "0400", "cpt", false),
                                        new DayDto("monday", "2200", "0200", "ets", false)),
                                "tuesday",
                                List.of(
                                        new DayDto("tuesday", "0800", "0600", "cpt", false),
                                        new DayDto("tuesday", "0800", "0400", "cpt", false),
                                        new DayDto("tuesday", "0800", "0500", "cpt", false)),
                                "saturday",
                                List.of(new DayDto("saturday", "0800", "0400", "cpt", false))),
                        new Date(2020, 1, 20),
                        new Date(2021, 1, 21));

        final RouteEtsDto routeEtsDtoBadCanalization =
                new RouteEtsDto(
                        "ARBA01_I_931",
                        "ARBA01",
                        "X",
                        "831",
                        Map.of(
                                "monday",
                                List.of(
                                        new DayDto("monday", "2100", "0400", "cpt", false),
                                        new DayDto("monday", "2200", "0200", "ets", false)),
                                "tuesday",
                                List.of(
                                        new DayDto("tuesday", "0800", "0600", "cpt", false),
                                        new DayDto("tuesday", "0800", "0400", "cpt", false),
                                        new DayDto("tuesday", "0800", "0500", "cpt", false)),
                                "saturday",
                                List.of(new DayDto("saturday", "0800", "0400", "cpt", false))),
                        new Date(2020, 1, 20),
                        new Date(2021, 1, 21));

        final RouteEtsDto routeEtsDtoBadServiceId =
                new RouteEtsDto(
                        "ARBA01_I_931",
                        "ARBA01",
                        "X",
                        "8312",
                        Map.of(
                                "monday",
                                List.of(
                                        new DayDto("monday", "2100", "0400", "cpt", false),
                                        new DayDto("monday", "2200", "0200", "ets", false)),
                                "tuesday",
                                List.of(
                                        new DayDto("tuesday", "0800", "0600", "cpt", false),
                                        new DayDto("tuesday", "0800", "0400", "cpt", false),
                                        new DayDto("tuesday", "0800", "0500", "cpt", false)),
                                "saturday",
                                List.of(new DayDto("saturday", "0800", "0400", "cpt", false))),
                        new Date(2020, 1, 20),
                        new Date(2021, 1, 21));

        return List.of(routeEtsDto, routeEtsDto,
                routeEtsDtoBadCanalization, routeEtsDtoBadServiceId);
    }


    private List<RouteCoverageResult> mockResponseCoverageWithResult() {

        return List.of(
                new RouteCoverageResult(
                        new Canalization("X",
                                List.of(
                                        new CarrierServiceId("831"))),
                        "active"),
                new RouteCoverageResult(
                        new Canalization("X",
                                List.of(
                                        new CarrierServiceId("123"))),
                        "active"),
                new RouteCoverageResult(
                        new Canalization("12345",
                                List.of(
                                        new CarrierServiceId("158663"))),
                        "inactive"));

    }

}