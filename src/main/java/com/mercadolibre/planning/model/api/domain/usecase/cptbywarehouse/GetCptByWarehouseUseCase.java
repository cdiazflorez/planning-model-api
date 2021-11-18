package com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.api.client.rest.RouteEtsClient;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.DayDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.ProcessingTimeByDate;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@AllArgsConstructor
@SuppressWarnings("PMD.CloseResource")
@SuppressFBWarnings("OCP_OVERLY_CONCRETE_PARAMETER")
public class GetCptByWarehouseUseCase
        implements UseCase<GetCptByWarehouseInput, List<GetCptByWarehouseOutput>> {

    private static final int MINUTES_IN_HOUR = 60;

    private static final int SECONDS = 60;

    private RouteEtsClient routeEtsClient;

    @Override
    public List<GetCptByWarehouseOutput> execute(final GetCptByWarehouseInput input) {
        final List<ProcessingTimeByDate> routes;
        try {
            routes = getRouteEtd(input);
        } catch (ClientException | NoEtdsFoundException e) {
            log.error("RouteApi error, run list cpt default", e);
            return getCptByZonedDateTimes(input);
        }
      
        return getCptByRouteEts(input, routes);
    }

    private List<GetCptByWarehouseOutput> getCptByZonedDateTimes(
            final GetCptByWarehouseInput input) {

        final ProcessingTime ptDefault = new ProcessingTime(240, MetricUnit.MINUTES);

        return input.getCptDefault().stream()
                .map(item -> GetCptByWarehouseOutput.builder()
                                        .date(item)
                                        .processingTime(ptDefault)
                                        .canalizationId(null)
                                        .logisticCenterId(input.getLogisticCenterId())
                                        .build())
                .collect(Collectors.toList());
    }

    private List<ProcessingTimeByDate> getRouteEtd(final GetCptByWarehouseInput input) {
        final List<RouteEtsDto> routes = routeEtsClient.postRoutEts(
                        RouteEtsRequest.builder()
                                .fromFilter(List.of(input.getLogisticCenterId()))
                                .build());

        if (routes.isEmpty()) {
            throw new NoEtdsFoundException("No available cpt found");
        }

        final Stream<DayDto> allCptDays =
                routes.stream()
                        .flatMap(route -> route.getFixedEtsByDay().values().stream())
                        .flatMap(Collection::stream);

        final Map<DayOfWeek, List<ProcessingTimeByDate>> distinctProcessingTimesByDate =
                allCptDays
                        .map(this::toProcessingTimeByDate)
                        .collect(
                                Collectors.groupingBy(
                                        ProcessingTimeByDate::getEtDay,
                                        HashMap::new,
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                this::selectSmallestProcessingTimes))
                        );

        return distinctProcessingTimesByDate.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private int getProcessingTimeId(final ProcessingTimeByDate processingTimeByDate) {
        return processingTimeByDate.getHour() * SECONDS + processingTimeByDate.getMinutes();
    }

    private List<ProcessingTimeByDate> selectSmallestProcessingTimes(
            final List<ProcessingTimeByDate> cptOutputAux) {

        return new ArrayList<>(
                cptOutputAux.stream()
                        .collect(
                                Collectors.toMap(
                                        this::getProcessingTimeId,
                                        Function.identity(),
                                        (a, b) -> a.getProcessingTime() < b.getProcessingTime()
                                                ? a
                                                : b
                                ))
                        .values());
    }

    private ProcessingTimeByDate toProcessingTimeByDate(final DayDto dayDto) {
        final int processingTime = Integer.parseInt(dayDto.getProcessingTime().substring(2))
                        + Integer.parseInt(dayDto.getProcessingTime().substring(0, 2))
                        * MINUTES_IN_HOUR;

        return new ProcessingTimeByDate(
                DayOfWeek.valueOf(dayDto.getEtDay().toUpperCase(Locale.ROOT)),
                Integer.parseInt(dayDto.getEtHour().substring(0, 2)),
                Integer.parseInt(dayDto.getEtHour().substring(2)),
                processingTime);
    }

    private boolean isBetweenInclusive(final ZonedDateTime date,
                                       final ZonedDateTime from,
                                       final ZonedDateTime to) {

        return date.equals(from) || date.equals(to) || date.isAfter(from) && date.isBefore(to);
    }

    private List<GetCptByWarehouseOutput> getCptByRouteEts(
            final GetCptByWarehouseInput input, final List<ProcessingTimeByDate> routes) {

        final ZonedDateTime cptFromTimeZoneWarehouse =
                getDateWithTimeZone(input.getCptFrom(), input.getTimeZone());

        return routes.stream()
                .map(route -> generateCptByWarehouseOutput(route, cptFromTimeZoneWarehouse))
                .filter(cpt ->
                        isBetweenInclusive(cpt.getDate(), input.getCptFrom(), input.getCptTo())
                )
                .sorted(Comparator.comparing(GetCptByWarehouseOutput::getDate))
                .collect(Collectors.toList());
    }

    private GetCptByWarehouseOutput generateCptByWarehouseOutput(
            final ProcessingTimeByDate route, final ZonedDateTime dateFromTimeZoneWarehouse) {

        final ZonedDateTime date = generateDate(route, dateFromTimeZoneWarehouse)
                .withZoneSameInstant(ZoneId.of("UTC"));

        return GetCptByWarehouseOutput.builder()
                .date(date)
                .processingTime(
                        new ProcessingTime(route.getProcessingTime(), MetricUnit.MINUTES)
                )
                .build();
    }

    private ZonedDateTime generateDate(
            final ProcessingTimeByDate route, final ZonedDateTime dateFrom) {

        final ZonedDateTime date =
                dateFrom
                        .with(TemporalAdjusters.nextOrSame(route.getEtDay()))
                        .withHour(route.getHour())
                        .withMinute(route.getMinutes());
        return date.isBefore(dateFrom) ? date.plusWeeks(1) : date;
    }

    private ZonedDateTime getDateWithTimeZone(final ZonedDateTime date, final String zoneId) {
        return date.withZoneSameInstant(ZoneId.of(zoneId));
    }
}
