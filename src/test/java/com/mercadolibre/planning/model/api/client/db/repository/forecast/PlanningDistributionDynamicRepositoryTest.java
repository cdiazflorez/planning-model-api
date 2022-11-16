package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.PROCESS_PATH;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistribution;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PlanningDistributionDynamicRepositoryTest {

  private static final Instant DATE_IN_FROM = Instant.parse("2022-11-09T00:00:00Z");
  private static final Instant DATE_IN_TO = Instant.parse("2022-11-11T00:00:00Z");
  private static final Instant DATE_OUT_FROM = Instant.parse("2022-11-11T00:00:00Z");
  private static final Instant DATE_OUT_TO = Instant.parse("2022-11-13T00:00:00Z");

  private static final Instant DATE_IN = Instant.parse("2022-11-09T10:00:00Z");
  private static final Instant DATE_IN_2 = Instant.parse("2022-11-10T11:00:00Z");
  private static final Instant DATE_OUT = Instant.parse("2022-11-11T10:00:00Z");
  private static final Instant DATE_OUT_2 = Instant.parse("2022-11-12T11:00:00Z");

  private static final String SQL = "/sql/forecast/load_planning_distribution.sql";

  @Autowired
  private PlanningDistributionDynamicRepository planningDistributionDynamicRepository;

  @Test
  @Sql({SQL})
  void testGetPlanningDistributionGroupByDateIn() {
    //WHEN
    final var result = planningDistributionDynamicRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        DATE_IN_FROM,
        DATE_IN_TO,
        DATE_OUT_FROM,
        DATE_OUT_TO,
        Set.of(ProcessPath.values()),
        Set.of(Grouper.DATE_IN),
        Set.of(1L, 2L, 3L)
    );

    //THEN
    final var expected = List.of(
        new PlanningDistribution(1L, DATE_IN, null, null, 300.0),
        new PlanningDistribution(1L, DATE_IN_2, null, null, 1060.5),
        new PlanningDistribution(2L, DATE_IN_2, null, null, 100.0)
    );

    assertionPlanningDistribution(expected, result);
  }

  @Test
  @Sql({SQL})
  void testGetPlanningDistributionGroupByDateOut() {
    //WHEN
    final var result = planningDistributionDynamicRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        DATE_IN_FROM,
        DATE_IN_TO,
        DATE_OUT_FROM,
        DATE_OUT_TO,
        Set.of(ProcessPath.values()),
        Set.of(Grouper.DATE_OUT),
        Set.of(1L, 3L)
    );

    //THEN
    final var expected = List.of(
        new PlanningDistribution(1L, null, DATE_OUT, null, 300.0),
        new PlanningDistribution(1L, null, DATE_OUT_2, null, 1060.5)
    );

    assertionPlanningDistribution(expected, result);
  }

  @Test
  @Sql({SQL})
  void testGetPlanningDistributionGroupByProcessPath() {
    //WHEN
    final var result = planningDistributionDynamicRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        DATE_IN_FROM,
        DATE_IN_TO,
        DATE_OUT_FROM,
        DATE_OUT_TO,
        Set.of(ProcessPath.values()),
        Set.of(PROCESS_PATH),
        Set.of(1L)
    );
    //THEN
    final var expected = List.of(
        new PlanningDistribution(1L, null, null, GLOBAL, 1000.0),
        new PlanningDistribution(1L, null, null, NON_TOT_MONO, 210.5),
        new PlanningDistribution(1L, null, null, TOT_MONO, 150.0)
    );

    assertionPlanningDistribution(expected, result);
  }

  @Test
  @Sql({SQL})
  void testGetPlanningDistributionGroupByDateInDateOutAndProcessPath() {
    //WHEN
    final var result = planningDistributionDynamicRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        DATE_IN_FROM,
        DATE_IN_TO,
        DATE_OUT_FROM,
        DATE_OUT_TO,
        Set.of(ProcessPath.values()),
        Set.of(Grouper.DATE_IN, Grouper.DATE_OUT, PROCESS_PATH),
        Set.of(1L, 3L)
    );
    //THEN
    final var expected = List.of(
        new PlanningDistribution(1L, DATE_IN, DATE_OUT, NON_TOT_MONO, 200.0),
        new PlanningDistribution(1L, DATE_IN, DATE_OUT, TOT_MONO, 100.0),
        new PlanningDistribution(1L, DATE_IN_2, DATE_OUT_2, GLOBAL, 1000.0),
        new PlanningDistribution(1L, DATE_IN_2, DATE_OUT_2, NON_TOT_MONO, 10.5),
        new PlanningDistribution(1L, DATE_IN_2, DATE_OUT_2, TOT_MONO, 50.0)
    );

    assertionPlanningDistribution(
        expected.stream().sorted(Comparator.comparing(PlanningDistribution::getDateIn)).collect(Collectors.toList()),
        result.stream().sorted(Comparator.comparing(PlanningDistribution::getDateIn)).collect(Collectors.toList())
    );
  }

  @Test
  @Sql({SQL})
  void testGetPlanningDistributionForecastIdFake() {
    //WHEN
    final var result = planningDistributionDynamicRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        DATE_IN_FROM,
        DATE_IN_TO,
        DATE_OUT_FROM,
        DATE_OUT_TO,
        Set.of(ProcessPath.values()),
        Set.of(PROCESS_PATH),
        Set.of(500L)
    );
    //THEN
    assertEquals(emptyList(), result);
  }

  private void assertionPlanningDistribution(final List<PlanningDistribution> expected, final List<PlanningDistribution> actual) {
    assertEquals(expected.size(), actual.size());

    for (int i = 0; i < expected.size(); i++) {
      assertEquals(expected.get(i).getDateIn(), actual.get(i).getDateIn());
      assertEquals(expected.get(i).getDateOut(), actual.get(i).getDateOut());
      assertEquals(expected.get(i).getProcessPath(), actual.get(i).getProcessPath());
      assertEquals(expected.get(i).getQuantity(), actual.get(i).getQuantity());
    }
  }
}
