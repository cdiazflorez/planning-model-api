package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetCptProjectionInput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput.fromEntityOutputs;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.CPT;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
@AllArgsConstructor
@SuppressWarnings("PMD.ExcessiveImports")
public class GetCptProjectionUseCase {

    private final CalculateCptProjectionUseCase calculateCptProjection;

    private final GetThroughputUseCase getThroughputUseCase;

    private final GetPlanningDistributionUseCase getPlanningUseCase;

    private final GetCapacityPerHourUseCase getCapacityPerHourUseCase;

    private final GetCptByWarehouseUseCase getCptByWarehouseUseCase;

    public List<CptProjectionOutput> execute(final GetCptProjectionInput request) {
        final Workflow workflow = request.getWorkflow();
        final String warehouseId = request.getWarehouseId();
        final ZonedDateTime dateFrom = request.getDateFrom();
        final ZonedDateTime dateTo = request.getDateTo();
        final String timeZone = request.getTimeZone();

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
                .builder()
                .warehouseId(warehouseId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(request.getProcessName())
                .workflow(workflow)
                .build());

        final Map<ZonedDateTime, Integer> capacity = getCapacityPerHourUseCase
                .execute(fromEntityOutputs(throughput))
                .stream()
                .collect(toMap(
                        CapacityOutput::getDate,
                        capacityOutput -> (int) capacityOutput.getValue()
                ));

        final List<GetPlanningDistributionOutput> planningUnits = getPlanningUseCase.execute(
                GetPlanningDistributionInput.builder()
                        .workflow(workflow)
                        .warehouseId(request.getWarehouseId())
                        .dateOutFrom(request.getDateFrom())
                        .dateOutTo(request.getDateTo())
                        .applyDeviation(request.isApplyDeviation())
                        .build());

        final List<GetCptByWarehouseOutput> cptByWarehouse = getCptByWarehouseUseCase
                .execute(new GetCptByWarehouseInput(warehouseId, dateFrom, dateTo,
                        getCptDefaultFromBacklog(request.getBacklog()), timeZone));

        final List<CptCalculationOutput> cptProjectionOutputs =
                calculateCptProjection.execute(CptProjectionInput.builder()
                        .workflow(workflow)
                        .logisticCenterId(warehouseId)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .capacity(capacity)
                        .backlog(getBacklog(request.getBacklog()))
                        .planningUnits(planningUnits)
                        .projectionType(CPT)
                        .cptByWarehouse(cptByWarehouse)
                        .currentDate(getCurrentUtcDate())
                        .build());

        return cptProjectionOutputs.stream()
                .map(item -> new CptProjectionOutput(
                        item.getDate(),
                        item.getProjectedEndDate(),
                        item.getRemainingQuantity())
                ).collect(toList());
    }

    private List<Backlog> getBacklog(final List<QuantityByDate> backlogs) {
        return backlogs == null
                ? emptyList()
                : backlogs.stream().map(QuantityByDate::toBacklog).collect(toList());
    }

    private List<ZonedDateTime> getCptDefaultFromBacklog(final List<QuantityByDate> backlogs) {
        return backlogs == null
                ? emptyList()
                : backlogs.stream().map(QuantityByDate::getDate).distinct().collect(toList());
    }
}
