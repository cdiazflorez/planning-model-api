package com.mercadolibre.planning.model.api.domain.usecase.sla;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.sla.Canalization;
import com.mercadolibre.planning.model.api.domain.entity.sla.CarrierServiceId;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.entity.sla.RouteCoverageResult;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.DayDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.ProcessingTimeByDate;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;
import com.mercadolibre.planning.model.api.gateway.RouteCoverageClientGateway;
import com.mercadolibre.planning.model.api.gateway.RouteEtsGateway;
import com.mercadolibre.planning.model.api.util.DateUtils;
import com.mercadolibre.planning.model.api.util.GetSlaByWarehouseUtils;
import com.newrelic.api.agent.Trace;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class GetSlaByWarehouseOutboundService implements GetSlaByWarehouseService {

  private static final int MINUTES_IN_HOUR = 60;

  private static final int SECONDS = 60;

  private RouteEtsGateway routeEtsGateway;

  private RouteCoverageClientGateway routeCoverageClientGateway;

  @Trace
  @Override
  public List<GetSlaByWarehouseOutput> execute(final GetSlaByWarehouseInput input) {

    final List<ProcessingTimeByDate> routes = generateSlaByWarehouse(input);

    final List<GetSlaByWarehouseOutput> slasRoute = getSlaByRouteEts(input, routes);

    log.info("{} route ets processing times: {}", input.getLogisticCenterId(), slasRoute);

    //TODO: if addBacklogInSlaInOrder will not be used in inbound,
    // move code to this method and delete util
    return GetSlaByWarehouseUtils
        .addBacklogInSlaInOrder(generateSlaByBacklog(input), slasRoute);
  }


  private List<ProcessingTimeByDate> generateSlaByWarehouse(final GetSlaByWarehouseInput input) {
    try {
      return getRouteEtd(input);
    } catch (ClientException | NoEtdsFoundException e) {
      log.error("RouteApi error, run list cpt default", e);
      return Collections.emptyList();
    }
  }

  private List<GetSlaByWarehouseOutput> generateSlaByBacklog(
      final GetSlaByWarehouseInput input) {

    final ProcessingTime ptDefault = new ProcessingTime(240, MetricUnit.MINUTES);

    return input.getDafaultSlas() == null
        ? Collections.emptyList()
        : input.getDafaultSlas().stream()
        .sorted()
        .map(item -> GetSlaByWarehouseOutput.builder()
            .date(item)
            .processingTime(ptDefault)
            .logisticCenterId(input.getLogisticCenterId())
            .build())
        .collect(Collectors.toList());
  }

  private List<ProcessingTimeByDate> getRouteEtd(final GetSlaByWarehouseInput input) {
    final List<RouteEtsDto> routesAux = routeEtsGateway.postRoutEts(
        RouteEtsRequest.builder()
            .fromFilter(List.of(input.getLogisticCenterId()))
            .build());

    if (routesAux.isEmpty()) {
      throw new NoEtdsFoundException("No available cpt found");
    }

    final List<RouteEtsDto> routes = filterActiveCpts(routesAux,
        input.getLogisticCenterId());

    final Stream<DayDto> allCptDays =
        routes.stream()
            .flatMap(route -> route.getFixedEtsByDay().values().stream()
                .filter(Objects::nonNull))
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

  private List<RouteEtsDto> filterActiveCpts(
      final List<RouteEtsDto> routes,
      final String logisticCenterId) {

    final List<RouteCoverageResult> routeCoverageResults = routeCoverageClientGateway
        .get(logisticCenterId);

    final Map<String, List<String>> routeCoverageMap = routeCoverageResults.stream()
        .map(RouteCoverageResult::getCanalization)
        .collect(Collectors.toMap(
            Canalization::getId,
            canalization ->
                canalization.getCarrierServices()
                    .stream()
                    .map(CarrierServiceId::getId)
                    .collect(Collectors.toList()),
            (serviceIdA, serviceIdB) -> {
              List<String> services = new ArrayList<>(serviceIdA);
              services.addAll(serviceIdB);
              return services;
            }

        ));

    return routes.stream()
        .filter(r ->
            routeCoverageMap.get(r.getCanalization()) != null
                && routeCoverageMap.get(r.getCanalization())
                .contains(r.getServiceId()))
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

  private List<GetSlaByWarehouseOutput> getSlaByRouteEts(final GetSlaByWarehouseInput input,
                                                         final List<ProcessingTimeByDate> routes) {

    final ZonedDateTime cptFromTimeZoneWarehouse = getDateWithTimeZone(input.getCptFrom(), input.getTimeZone());

    return routes.stream()
        .map(route -> generateCptByWarehouseOutput(route, cptFromTimeZoneWarehouse))
        .filter(cpt ->
            DateUtils.isBetweenInclusive(cpt.getDate(), input.getCptFrom(), input.getCptTo())
        )
        .sorted(Comparator.comparing(GetSlaByWarehouseOutput::getDate))
        .collect(Collectors.toList());
  }

  private GetSlaByWarehouseOutput generateCptByWarehouseOutput(
      final ProcessingTimeByDate route, final ZonedDateTime dateFromTimeZoneWarehouse) {

    final ZonedDateTime date = generateDate(route, dateFromTimeZoneWarehouse)
        .withZoneSameInstant(ZoneId.of("UTC"));

    return GetSlaByWarehouseOutput.builder()
        .date(date)
        .processingTime(
            new ProcessingTime(route.getProcessingTime(), MetricUnit.MINUTES)
        )
        .build();
  }

  private ZonedDateTime generateDate(final ProcessingTimeByDate route, final ZonedDateTime dateFrom) {

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
