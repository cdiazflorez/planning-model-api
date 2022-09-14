package com.mercadolibre.planning.model.api.util;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput.builder;
import static com.mercadolibre.planning.model.api.util.DateUtils.ignoreMinutes;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CurrentBacklog;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class ProjectionTestsUtils {

    public static final ZonedDateTime A_FIXED_DATE = A_DATE_UTC.withFixedOffsetZone();

    public static List<EntityOutput> mockThroughputs() {
        return List.of(
                mockThroughputEntity(A_FIXED_DATE.minusHours(1), PICKING, 850),
                mockThroughputEntity(A_FIXED_DATE.minusHours(1), PACKING, 650),
                mockThroughputEntity(A_FIXED_DATE, PICKING, 800),
                mockThroughputEntity(A_FIXED_DATE, PACKING, 550),
                mockThroughputEntity(A_FIXED_DATE.plusHours(1), PICKING, 600),
                mockThroughputEntity(A_FIXED_DATE.plusHours(1), PACKING, 700),
                mockThroughputEntity(A_FIXED_DATE.plusHours(2), PICKING, 600),
                mockThroughputEntity(A_FIXED_DATE.plusHours(2), PACKING, 700),
                mockThroughputEntity(A_FIXED_DATE.plusHours(3), PICKING, 0),
                mockThroughputEntity(A_FIXED_DATE.plusHours(3), PACKING, 50),
                mockThroughputEntity(A_FIXED_DATE.plusHours(4), PICKING, 1000),
                mockThroughputEntity(A_FIXED_DATE.plusHours(4), PACKING, 910),
                mockThroughputEntity(A_FIXED_DATE.minusHours(1), BATCH_SORTER, 650),
                mockThroughputEntity(A_FIXED_DATE, BATCH_SORTER, 550),
                mockThroughputEntity(A_FIXED_DATE.plusHours(1), BATCH_SORTER, 600),
                mockThroughputEntity(A_FIXED_DATE.plusHours(2), BATCH_SORTER, 700),
                mockThroughputEntity(A_FIXED_DATE.plusHours(3), BATCH_SORTER, 350),
                mockThroughputEntity(A_FIXED_DATE.plusHours(4), BATCH_SORTER, 450)
            );
    }

    public static List<EntityOutput> getMinCapacity() {
        return List.of(
                mockThroughputEntity(A_FIXED_DATE.minusHours(1), PACKING, 650),
                mockThroughputEntity(A_FIXED_DATE, PACKING, 550),
                mockThroughputEntity(A_FIXED_DATE.plusHours(1), PICKING, 600),
                mockThroughputEntity(A_FIXED_DATE.plusHours(2), PICKING, 600),
                mockThroughputEntity(A_FIXED_DATE.plusHours(3), PICKING, 0),
                mockThroughputEntity(A_FIXED_DATE.plusHours(4), PACKING, 910)
        );
    }

    public static EntityOutput mockThroughputEntity(final ZonedDateTime date,
                                                     final ProcessName processName,
                                                     final long quantity) {
        return EntityOutput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .metricUnit(UNITS_PER_HOUR)
                .source(FORECAST)
                .date(date)
                .processName(processName)
                .value(quantity)
                .build();
    }

    public static Map<ZonedDateTime, Long> mockPlanningSalesByDate() {
        return mockPlanningDistributionOutputs().stream()
                .collect(toMap(o -> ignoreMinutes(o.getDateIn()),
                        GetPlanningDistributionOutput::getTotal,
                        Long::sum));
    }

    public static BacklogProjectionInput mockBacklogProjectionInput(
            final List<ProcessName> processNames,
            final List<CurrentBacklog> backlogs,
            final ZonedDateTime dateTo) {

        return BacklogProjectionInput.builder()
                .processNames(processNames)
                .throughputs(mockThroughputs())
                .currentBacklogs(backlogs)
                .dateFrom(A_FIXED_DATE.minusMinutes(15))
                .dateTo(dateTo)
                .planningUnits(mockPlanningDistributionOutputs())
                .packingWallRatios(Map.of(A_FIXED_DATE, 1.00))
                .build();
    }

    public static List<GetPlanningDistributionOutput> mockPlanningDistributionOutputs() {
        return List.of(
                builder()
                        .dateOut(A_FIXED_DATE.plusHours(3))
                        .dateIn(A_FIXED_DATE.minusHours(1))
                        .total(200)
                        .build(),
                builder()
                        .dateOut(A_FIXED_DATE.plusHours(4))
                        .dateIn(A_FIXED_DATE)
                        .total(300)
                        .build(),
                builder()
                        .dateOut(A_FIXED_DATE.plusHours(5))
                        .dateIn(A_FIXED_DATE.plusHours(1))
                        .total(400)
                        .build(),
                builder()
                        .dateOut(A_FIXED_DATE.plusHours(5))
                        .dateIn(A_FIXED_DATE.plusHours(3))
                        .total(1400)
                        .build());
    }

    public static void assertCapacityByDate(final Map<ZonedDateTime, Long> capacityByDate,
                                            final List<EntityOutput> entityOutputs) {
        for (final EntityOutput output : entityOutputs) {
            assertTrue(capacityByDate.containsKey(output.getDate()));
            assertEquals(output.getValue(), capacityByDate.get(output.getDate()));
        }
    }
}
