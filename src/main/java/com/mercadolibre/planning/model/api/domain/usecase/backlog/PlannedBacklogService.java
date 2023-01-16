package com.mercadolibre.planning.model.api.domain.usecase.backlog;

import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.DATE_IN;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.WORKFLOW;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND_TRANSFER;
import static java.time.ZoneOffset.UTC;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.LastPhotoRequest;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.WorkflowService;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlannedBacklogService {

  private static final double DEVIATION_BASE = 1.0;

  private final PlanningDistributionService planningDistributionService;

  private final BacklogGateway backlogGateway;

  private final CurrentForecastDeviationRepository currentForecastDeviationRepository;

  private static ZonedDateTime parseDate(final String date) {
    return ZonedDateTime.parse(date).withZoneSameInstant(UTC);
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

  private class Delegate implements WorkflowService<Request, List<PlannedUnits>> {

    @Override
    public List<PlannedUnits> executeInbound(final Request request) {

      final Photo photo = backlogGateway.getLastPhoto(
          new LastPhotoRequest(
              List.of(INBOUND, INBOUND_TRANSFER),
              request.warehouseId,
              List.of("SCHEDULED"),
              null,
              null,
              null,
              null,
              request.getDateOutFrom().toInstant(),
              request.getDateOutTo().toInstant(),
              List.of(DATE_IN, DATE_OUT, WORKFLOW),
              request.getViewDate().toInstant()
          )
      );

      if (photo == null) {
        return Collections.emptyList();
      }

      if (request.isApplyDeviation()) {
        // WARNING: This only applies for deviations of type units
        final List<CurrentForecastDeviation> currentForecastDeviations =
            currentForecastDeviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowIn(
                request.getWarehouseId(),
                Set.of(INBOUND, INBOUND_TRANSFER)
            );

        if (currentForecastDeviations != null && !currentForecastDeviations.isEmpty()) {
          return fromGroupsToPlannedUnits(applyDeviationInbound(photo, currentForecastDeviations));
        }

      }

      return fromGroupsToPlannedUnits(photo.getGroups().stream());
    }

    private Stream<Photo.Group> applyDeviationInbound(final Photo photo, final List<CurrentForecastDeviation> currentForecastDeviations) {
      return photo.groups.stream()
          .map(group -> {
                final var deviations = getApplicableDeviations(
                    currentForecastDeviations, group.getKey().get(WORKFLOW.getName()), Instant.parse(group.getKey().get(DATE_IN.getName()))
                );

                final var deviation = deviations.stream()
                    .map(dev -> dev.getValue() + DEVIATION_BASE)
                    .reduce(1D, (x, y) -> x * y);

                return new Photo.Group(
                    group.getKey(), (int) Math.round(group.getTotal() * deviation), group.getAccumulatedTotal()
                );
              }
          );
    }

    private List<CurrentForecastDeviation> getApplicableDeviations(
        final List<CurrentForecastDeviation> deviations,
        final String workflowName,
        final Instant dateIn
    ) {

      final var workflow = Workflow.of(workflowName).orElseThrow();

      return deviations.stream().filter(
          deviation -> deviation.getWorkflow().equals(workflow)
              && DateUtils.isBetweenInclusive(deviation.getDateFrom(), dateIn, deviation.getDateTo())
      ).collect(Collectors.toList());
    }

    private List<PlannedUnits> fromGroupsToPlannedUnits(final Stream<Photo.Group> groups) {
      return groups.collect(
              Collectors.toMap(
                  group -> new KeyGroupAux(
                      parseDate(group.getKey().get(DATE_IN.getName())),
                      parseDate(group.getKey().get(DATE_OUT.getName()))
                  ),
                  group -> new PlannedUnits(
                      parseDate(group.getKey().get(DATE_IN.getName())),
                      parseDate(group.getKey().get(DATE_OUT.getName())),
                      group.getTotal()
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
          .dateOutFrom(request.getDateOutFrom())
          .dateOutTo(request.getDateOutTo())
          .applyDeviation(request.isApplyDeviation())
          .build();

      return planningDistributionService.getPlanningDistribution(input)
          .stream()
          .map(plannedUnits -> new PlannedUnits(
              plannedUnits.getDateIn(),
              plannedUnits.getDateOut(),
              plannedUnits.getTotal())
          ).collect(Collectors.toList());
    }
  }
}
