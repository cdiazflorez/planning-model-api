package com.mercadolibre.planning.model.api.web.controller.projection;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionByArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper.BacklogMapper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper.UnitsAreaDistributionMapper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogByArea;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.CurrentBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.PlannedBacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.ThroughputByHour;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.ProjectionResult;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.QuantityAtDate;
import com.mercadolibre.planning.model.api.web.controller.projection.request.AreaShareAtSlaAndProcessDto;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ThroughputDto;
import com.mercadolibre.planning.model.api.web.controller.projection.response.BacklogProjectionByAreaDto;
import com.mercadolibre.planning.model.api.web.controller.projection.response.BacklogProjectionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Entry point for backlog projection algorithms.
 *
 * <p>Provides input and output mapping facilities.</p>
 */
@Service
@AllArgsConstructor
public class BacklogProjectionAdapter {

  private BacklogProjectionByArea backlogProjectionByArea;

  public List<BacklogProjectionByAreaDto> projectionByArea(final Instant dateFrom,
                                                           final Instant dateTo,
                                                           final Workflow workflow,
                                                           final List<ProcessName> processName,
                                                           final List<ThroughputDto> throughput,
                                                           final List<GetPlanningDistributionOutput> planningUnits,
                                                           final List<CurrentBacklogBySla> currentBacklog,
                                                           final List<AreaShareAtSlaAndProcessDto> areaDistributions) {

    return mapResponse(backlogProjectionByArea.execute(
        dateFrom,
        dateTo,
        workflow,
        processName,
        getThroughputByProcess(throughput),
        getIncomingBacklog(planningUnits),
        getCurrentBacklog(currentBacklog),
        getMapper(areaDistributions)
    ));
  }


  private Map<ProcessName, ThroughputByHour> getThroughputByProcess(final List<ThroughputDto> throughput) {
    return throughput.stream()
        .collect(groupingBy(
            ThroughputDto::getProcessName,
            Collectors.collectingAndThen(
                toMap(
                    ThroughputDto::getDate,
                    entity -> (int) entity.getValue()
                ),
                ThroughputByHour::new
            )
        ));
  }

  private PlannedBacklogBySla getIncomingBacklog(final List<GetPlanningDistributionOutput> planningUnits) {
    final var distributionsByDate = planningUnits.stream()
        .collect(groupingBy(
                distribution -> distribution.getDateIn().toInstant(),
                toList()
            )
        )
        .entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> {
              final var quantities = entry.getValue()
                  .stream()
                  .map(distribution -> new QuantityAtDate(distribution.getDateOut().toInstant(), (int) distribution.getTotal()))
                  .collect(toList());

              return new BacklogBySla(quantities);
            }
        ));

    return new PlannedBacklogBySla(distributionsByDate);
  }

  private Map<ProcessName, BacklogBySla> getCurrentBacklog(final List<CurrentBacklogBySla> currentBacklogs) {
    return currentBacklogs.stream()
        .collect(groupingBy(
            CurrentBacklogBySla::getProcessName,
            toList()
        ))
        .entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> {
              final var distributions = entry.getValue()
                  .stream()
                  .map(d -> new QuantityAtDate(d.getDateOut(), d.getQuantity()))
                  .collect(toList());

              return new BacklogBySla(distributions);
            }
        ));
  }

  private BacklogMapper<BacklogBySla, BacklogByArea> getMapper(final List<AreaShareAtSlaAndProcessDto> distributions) {
    final var distributionsMap = distributions.stream()
        .collect(groupingBy(
            AreaShareAtSlaAndProcessDto::getProcessName,
            groupingBy(
                AreaShareAtSlaAndProcessDto::getDate,
                toMap(
                    AreaShareAtSlaAndProcessDto::getArea,
                    AreaShareAtSlaAndProcessDto::getPercentage
                )
            )
        ));

    return new UnitsAreaDistributionMapper(distributionsMap);
  }

  private List<BacklogProjectionByAreaDto> mapResponse(final Map<ProcessName, List<ProjectionResult<BacklogByArea>>> projectionByProcess) {
    return projectionByProcess.entrySet()
        .stream()
        .flatMap(entry -> flattenProjections(entry.getKey(), entry.getValue()))
        .collect(toList());
  }

  private Stream<BacklogProjectionByAreaDto> flattenProjections(final ProcessName process,
                                                                final List<ProjectionResult<BacklogByArea>> projections) {

    return projections.stream()
        .flatMap(result -> Stream.concat(
            mapQuantitiesByArea(process, result.getOperatingHour(), result.getResultingState().getProcessed(),
                BacklogProjectionStatus.PROCESSED),
            mapQuantitiesByArea(process, result.getOperatingHour(), result.getResultingState().getCarryOver(),
                BacklogProjectionStatus.CARRY_OVER)
        ));
  }

  private Stream<BacklogProjectionByAreaDto> mapQuantitiesByArea(final ProcessName process,
                                                                 final Instant operatingHour,
                                                                 final BacklogByArea state,
                                                                 final BacklogProjectionStatus status) {
    return state.getQuantityByArea()
        .stream()
        .map(backlog ->
            new BacklogProjectionByAreaDto(
                operatingHour,
                process,
                backlog.getArea(),
                status,
                (long) backlog.getQuantity()
            )
        );
  }
}
