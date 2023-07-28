package com.mercadolibre.planning.model.api.domain.usecase.backlog;

import static com.mercadolibre.planning.model.api.domain.entity.DeviationType.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND_TRANSFER;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.WorkflowService;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

/**
 * Returns Planned Units for Inbound and Outbound
 *
 * <p>Inbound input are Scheduled Backlog.</p>
 * <p>Outbound input are Planning distribution (forecast).</p>
 */
@Service
@AllArgsConstructor
public class PlannedBacklogService {

  private static final double DEVIATION_BASE = 1.0;

  private final PlanningDistributionService planningDistributionService;

  private final InboundScheduledBacklogGateway inboundScheduledBacklogGateway;

  private final CurrentForecastDeviationRepository currentForecastDeviationRepository;

  private static ZonedDateTime parseDate(final Instant date) {
    return date.atZone(UTC);
  }

  public List<PlannedUnits> getExpectedBacklog(final String warehouseId,
                                               final Workflow workflow,
                                               final ZonedDateTime dateOutFrom,
                                               final ZonedDateTime dateOutTo,
                                               final ZonedDateTime viewDate,
                                               final boolean applyDeviation) {
    return workflow.execute(
        new Delegate(),
        new Request(warehouseId, workflow, dateOutFrom, dateOutTo, viewDate, applyDeviation)
    );
  }

  /**
   * Provides a gateway to return InboundScheduledBacklog.
   *
   * <p>Used to apply inbound deviations<p/>
   */
  public interface InboundScheduledBacklogGateway {

    List<InboundScheduledBacklog> getScheduledBacklog(
        String warehouseId,
        List<Workflow> workflows,
        Instant dateFrom,
        Instant dateTo,
        Instant viewDate
    );

  }

  @Value
  private static class Request {
    String warehouseId;

    Workflow workflow;

    ZonedDateTime dateOutFrom;

    ZonedDateTime dateOutTo;

    ZonedDateTime viewDate;

    boolean applyDeviation;
  }

  @Value
  private static class KeyGroupAux {
    ZonedDateTime dateIn;

    ZonedDateTime dateOut;
  }

  @Value
  public static class InboundScheduledBacklog {
    String workflow;
    Instant dateIn;
    Instant dateOut;
    Path path;
    int total;
    double accumulatedTotal;
  }

  private class Delegate implements WorkflowService<Request, List<PlannedUnits>> {

    @Override
    public List<PlannedUnits> executeInbound(final Request request) {

      final List<Workflow> workflows = request.getWorkflow().equals(FBM_WMS_INBOUND) ? List.of(INBOUND, INBOUND_TRANSFER)
          : List.of(request.getWorkflow());

      final List<InboundScheduledBacklog> scheduledBacklogs = inboundScheduledBacklogGateway.getScheduledBacklog(
          request.getWarehouseId(),
          workflows,
          request.getDateOutFrom().toInstant(),
          request.getDateOutTo().toInstant(),
          request.getViewDate().toInstant()
      );

      if (request.isApplyDeviation()) {
        final List<CurrentForecastDeviation> currentForecastDeviations =
            currentForecastDeviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowIn(
                request.getWarehouseId(),
                Set.copyOf(workflows)
            );

        if (currentForecastDeviations != null && !currentForecastDeviations.isEmpty()) {
          //Deviations need to be applied in the same order they were created
          final List<CurrentForecastDeviation> sortedDeviations = currentForecastDeviations.stream()
              .sorted(Comparator.comparing(CurrentForecastDeviation::getDateCreated))
              .collect(Collectors.toList());
          return fromBacklogToPlannedUnits(getDeviatedBacklog(scheduledBacklogs, sortedDeviations));
        }
      }
      return fromBacklogToPlannedUnits(scheduledBacklogs);
    }

    private List<InboundScheduledBacklog> getDeviatedBacklog(
        final List<InboundScheduledBacklog> inboundScheduledBacklogs,
        final List<CurrentForecastDeviation> currentForecastDeviations
    ) {
      AtomicReference<List<InboundScheduledBacklog>> backlogReference = new AtomicReference<>(inboundScheduledBacklogs);
      currentForecastDeviations.forEach(dev -> backlogReference.set(backlogReference.get().stream()
          .map(backlog -> applyInboundDeviation(dev, backlog))
          .collect(Collectors.toList())));
      return backlogReference.get();
    }

    private InboundScheduledBacklog applyInboundDeviation(
        final CurrentForecastDeviation deviation,
        final InboundScheduledBacklog scheduledBacklog
    ) {
      if (isApplicableDeviation(deviation, scheduledBacklog.getWorkflow(), scheduledBacklog.getDateIn(), scheduledBacklog.getPath())) {
        return deviation.getType().equals(UNITS)
            ? applyUnitsDeviation(deviation.getValue(), scheduledBacklog)
            : applyTimeDeviation(deviation.getValue(), scheduledBacklog);
      }
      return scheduledBacklog;
    }

    private InboundScheduledBacklog applyUnitsDeviation(final double deviationValue, final InboundScheduledBacklog backlog) {
      final double deviationUnits = deviationValue + DEVIATION_BASE;
      return new InboundScheduledBacklog(
          backlog.workflow,
          backlog.dateIn,
          backlog.dateOut,
          backlog.path,
          (int) Math.round(backlog.getTotal() * deviationUnits),
          backlog.accumulatedTotal
      );
    }

    private InboundScheduledBacklog applyTimeDeviation(final double deviationValue, final InboundScheduledBacklog backlog) {
      final Instant dateInDeviation = backlog.getDateIn().plus((int) deviationValue, ChronoUnit.MINUTES);
      final Instant dateOutDeviation = backlog.getDateOut().plus((int) deviationValue, ChronoUnit.MINUTES);
      return new InboundScheduledBacklog(
          backlog.workflow,
          dateInDeviation,
          dateOutDeviation,
          backlog.path,
          backlog.getTotal(),
          backlog.accumulatedTotal
      );
    }

    private boolean isApplicableDeviation(
        final CurrentForecastDeviation deviation,
        final String workflowName,
        final Instant dateIn,
        final Path path
    ) {

      final var workflow = Workflow.of(workflowName).orElseThrow();
      if (workflow.equals(INBOUND)) {
        return deviation.getWorkflow().equals(workflow)
            && DateUtils.isBetweenInclusive(deviation.getDateFrom(), dateIn, deviation.getDateTo())
            && (deviation.getType().equals(UNITS) || deviation.getPath().equals(path));
      } else {
        return deviation.getWorkflow().equals(workflow)
            && DateUtils.isBetweenInclusive(deviation.getDateFrom(), dateIn, deviation.getDateTo());
      }
    }

    private List<PlannedUnits> fromBacklogToPlannedUnits(final List<InboundScheduledBacklog> scheduledBacklogs) {
      return scheduledBacklogs.stream().collect(
              Collectors.toMap(
                  backlog -> new KeyGroupAux(
                      parseDate(backlog.getDateIn()),
                      parseDate(backlog.getDateOut())
                  ),
                  backlog -> new PlannedUnits(
                      parseDate(backlog.getDateIn()),
                      parseDate(backlog.getDateOut()),
                      backlog.getTotal()
                  ),
                  (plannedUnits1, plannedUnits2) -> new PlannedUnits(
                      plannedUnits1.getDateIn(),
                      plannedUnits1.getDateOut(),
                      plannedUnits1.getTotal() + plannedUnits2.getTotal()
                  )
              )
          ).values().stream().sorted(Comparator.comparing(PlannedUnits::getDateIn))
          .collect(Collectors.toList());
    }

    @Override
    public List<PlannedUnits> executeOutbound(final Request request) {
      final GetPlanningDistributionInput input = GetPlanningDistributionInput.builder()
          .warehouseId(request.getWarehouseId())
          .workflow(request.getWorkflow())
          .dateOutFrom(request.getDateOutFrom().toInstant())
          .dateOutTo(request.getDateOutTo().toInstant())
          .applyDeviation(request.isApplyDeviation())
          .build();

      final Map<Pair<Instant, Instant>, Double> quantityByDatesPair = planningDistributionService.getPlanningDistribution(input)
          .stream()
          .collect(
              Collectors.groupingBy(
                  planningDistributionOutput -> Pair.of(
                      planningDistributionOutput.getDateIn(),
                      planningDistributionOutput.getDateOut()),
                  Collectors.summingDouble(GetPlanningDistributionOutput::getTotal))
          );

      return quantityByDatesPair.entrySet().stream()
          .map(entry -> new PlannedUnits(
              ofInstant(entry.getKey().getLeft(), UTC),
              ofInstant(entry.getKey().getRight(), UTC),
              Math.round(entry.getValue()))
          ).collect(Collectors.toList());
    }
  }
}
