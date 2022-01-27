package com.mercadolibre.planning.model.api.domain.usecase.backlog;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.WorkflowService;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;
import static java.util.List.of;

@Service
@AllArgsConstructor
public class PlannedBacklogService {

    private final PlanningDistributionService planningDistributionService;

    private final BacklogGateway backlogApiGateway;

    private static ZonedDateTime parseDate(final String date) {
        return ZonedDateTime.parse(date).withZoneSameInstant(UTC);
    }

    public List<PlannedUnits> getExpectedBacklog(final String warehouseId,
                                                 final Workflow workflow,
                                                 final ZonedDateTime dateOutFrom,
                                                 final ZonedDateTime dateOutTo,
                                                 final boolean applyDeviation) {
        return workflow.execute(
                new Delegate(),
                new Request(warehouseId, workflow, dateOutFrom, dateOutTo, applyDeviation)
        );
    }

    private class Delegate implements WorkflowService<Request, List<PlannedUnits>> {

        @Override
        public List<PlannedUnits> executeInbound(final Request request) {
            final List<BacklogPhoto> currentBacklog = backlogApiGateway.getCurrentBacklog(
                    request.getWarehouseId(),
                    of(request.getWorkflow()),
                    of("SCHEDULED"),
                    request.getDateOutFrom().toInstant(),
                    request.getDateOutTo().toInstant(),
                    of("date_in", "date_out")
            );

            return currentBacklog.stream()
                    .map(consolidation -> new PlannedUnits(
                            parseDate(consolidation.getKeys().get("date_in")),
                            parseDate(consolidation.getKeys().get("date_out")),
                            consolidation.getTotal()
                    ))
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

    @Value
    private static class Request {
        private String warehouseId;
        private Workflow workflow;
        private ZonedDateTime dateOutFrom;
        private ZonedDateTime dateOutTo;
        private boolean applyDeviation;
    }
}
