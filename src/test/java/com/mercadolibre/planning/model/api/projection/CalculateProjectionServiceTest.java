package com.mercadolibre.planning.model.api.projection;


import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.projection.dto.ProjectionRequest;
import com.mercadolibre.planning.model.api.projection.dto.ProjectionResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CalculateProjectionServiceTest {

  private static final Instant CURRENT_DATE = Instant.parse("2022-08-21T21:00:00Z");

  private static final Instant FIRST_OPERATION_DATE = CURRENT_DATE.plus(1, ChronoUnit.HOURS);

  private static final Instant SECOND_OPERATION_DATE = FIRST_OPERATION_DATE.plus(1, ChronoUnit.HOURS);

  private static final Instant THIRD_OPERATION_DATE = SECOND_OPERATION_DATE.plus(1, ChronoUnit.HOURS);

  private static final Instant FIRST_DATE_CPT = Instant.parse("2022-08-22T00:00:00Z");

  private static final Instant SECOND_DATE_CPT = FIRST_DATE_CPT.plus(1, ChronoUnit.HOURS);

  private static final Instant THIRD_DATE_CPT = SECOND_DATE_CPT.plus(2, ChronoUnit.HOURS);

  private static final Instant DATE_TO = THIRD_DATE_CPT.plus(3, ChronoUnit.HOURS);

  private static final ZoneId UTC = ZoneId.of("UTC");

  @InjectMocks
  private CalculateProjectionService calculateProjectionService;

  private static ProjectionRequest.PlanningDistribution planningDistribution(final Instant dateIn, final Instant dateOut, final int total) {
    return new ProjectionRequest.PlanningDistribution(dateIn.atZone(UTC), dateOut.atZone(UTC), total);
  }

  @Test
  void calculateProjectionServiceTest() {
    //GIVEN
    final ProjectionRequest projectionRequest = ProjectionRequest.builder()
        .dateFrom(CURRENT_DATE)
        .dateTo(DATE_TO)
        .throughputByProcess(throughputByProcess())
        .backlogBySlaAndProcess(currentBacklog())
        .forecastSales(forecastSales())
        .ratioByHour(ratioByHour())
        .build();

    // WHEN
    final List<ProjectionResult> projection = calculateProjectionService.execute(FBM_WMS_OUTBOUND, projectionRequest);

    List<ProjectionResult> sortedProjection = projection.stream()
        .sorted(Comparator.comparing(ProjectionResult::getDateOut))
        .collect(Collectors.toList());

    // THEN
    assertEquals(expected(), sortedProjection);
  }

  private List<ProjectionResult> expected() {
    return List.of(
        new ProjectionResult(
            FIRST_DATE_CPT,
            null,
            Instant.parse("2022-08-22T01:41:15Z"),
            675
        ),
        new ProjectionResult(
            SECOND_DATE_CPT,
            null,
            Instant.parse("2022-08-22T05:18:45Z"),
            2150
        ),
        new ProjectionResult(
            THIRD_DATE_CPT,
            null,
            null,
            2125
        )
    );
  }

  private Map<Instant, ProjectionRequest.PackingRatio> ratioByHour() {
    return Map.of(
        FIRST_OPERATION_DATE, new ProjectionRequest.PackingRatio(0.5, 0.5),
        SECOND_OPERATION_DATE, new ProjectionRequest.PackingRatio(0.5, 0.5)
    );
  }

  private Map<Instant, Integer> fill(final List<Instant> ips, final int value) {
    return ips.stream()
        .collect(Collectors.toMap(
            Function.identity(),
            ip -> value
        ));
  }

  private Map<ProcessName, Map<Instant, Integer>> throughputByProcess() {
    final var hours = ChronoUnit.HOURS.between(CURRENT_DATE, DATE_TO);
    final var ips = LongStream.rangeClosed(0, hours)
        .mapToObj(i -> CURRENT_DATE.plus(i, ChronoUnit.HOURS))
        .collect(Collectors.toList());

    return Map.of(
        WAVING, fill(ips, 600),
        PICKING, fill(ips, 600),
        BATCH_SORTER, fill(ips, 600),
        WALL_IN, fill(ips, 600),
        PACKING_WALL, fill(ips, 400),
        PACKING, fill(ips, 400)
    );
  }

  private Map<ProcessName, Map<Instant, Integer>> currentBacklog() {
    return Map.of(
        WAVING, Map.of(
            FIRST_DATE_CPT, 700,
            SECOND_DATE_CPT, 600,
            THIRD_DATE_CPT, 500
        ),
        PICKING, Map.of(
            FIRST_DATE_CPT, 250,
            SECOND_DATE_CPT, 450,
            THIRD_DATE_CPT, 300
        ),
        BATCH_SORTER, Map.of(
            FIRST_DATE_CPT, 300,
            SECOND_DATE_CPT, 200,
            THIRD_DATE_CPT, 150
        ),
        WALL_IN, Map.of(
            FIRST_DATE_CPT, 400,
            SECOND_DATE_CPT, 300,
            THIRD_DATE_CPT, 350
        ),
        PACKING_WALL, Map.of(
            FIRST_DATE_CPT, 300,
            SECOND_DATE_CPT, 200,
            THIRD_DATE_CPT, 150
        ),
        PACKING, Map.of(
            FIRST_DATE_CPT, 300,
            SECOND_DATE_CPT, 200,
            THIRD_DATE_CPT, 150
        )

    );
  }

  private List<ProjectionRequest.PlanningDistribution> forecastSales() {
    return List.of(
        planningDistribution(FIRST_OPERATION_DATE, FIRST_DATE_CPT, 200),
        planningDistribution(FIRST_OPERATION_DATE, SECOND_DATE_CPT, 450),
        planningDistribution(FIRST_OPERATION_DATE, THIRD_DATE_CPT, 500),
        planningDistribution(SECOND_OPERATION_DATE, FIRST_DATE_CPT, 300),
        planningDistribution(SECOND_OPERATION_DATE, SECOND_DATE_CPT, 300),
        planningDistribution(SECOND_OPERATION_DATE, THIRD_DATE_CPT, 200),
        planningDistribution(THIRD_OPERATION_DATE, FIRST_DATE_CPT, 2),
        planningDistribution(THIRD_OPERATION_DATE, SECOND_DATE_CPT, 1),
        planningDistribution(THIRD_OPERATION_DATE, THIRD_DATE_CPT, 1)
    );
  }

}


