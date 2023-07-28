package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanDistribution;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PlanningDistributionDynamicRepositoryTest {

  private static final Instant DATE_IN_FROM = Instant.parse("2022-11-10T00:00:00Z");
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

  @ParameterizedTest
  @MethodSource("provideProcessPathsArguments")
  @Sql({SQL})
  void testGetPlanningDistribution(final Instant dateInFrom,
                                   final Instant dateInTo,
                                   final Set<ProcessPath> processPaths,
                                   final Set<Long> forecastIds,
                                   final List<PlanDistribution> expected) {
    //GIVEN

    //WHEN

    //THEN
    final var result = planningDistributionDynamicRepository.findByForecastIdsAndDynamicFilters(
        dateInFrom,
        dateInTo,
        DATE_OUT_FROM,
        DATE_OUT_TO,
        processPaths,
        forecastIds
    );

    assertEquals(expected.size(), result.size());
    assertTrue(expected.containsAll(result) && result.containsAll(expected));
  }

  @Test
  @Sql({SQL})
  void testGetPlanningDistributionForecastIdFake() {
    //WHEN
    final var result = planningDistributionDynamicRepository.findByForecastIdsAndDynamicFilters(
        DATE_IN_FROM,
        DATE_IN_TO,
        DATE_OUT_FROM,
        DATE_OUT_TO,
        Set.of(ProcessPath.values()),
        Set.of(500L)
    );
    //THEN
    assertEquals(emptyList(), result);
  }

  private static Stream<Arguments> provideProcessPathsArguments() {
    return Stream.of(
        Arguments.of(
            null,
            DATE_IN_TO,
            Set.of(NON_TOT_MONO, TOT_MONO, GLOBAL),
            Set.of(1L, 2L),
            List.of(
                new PlanDistribution(1, DATE_IN, DATE_OUT, NON_TOT_MONO, UNITS, 200.0D),
                new PlanDistribution(1, DATE_IN, DATE_OUT, TOT_MONO, UNITS, 100.0D),
                new PlanDistribution(1, DATE_IN_2, DATE_OUT_2, GLOBAL, UNITS, 1000.0D),
                new PlanDistribution(1, DATE_IN_2, DATE_OUT_2, NON_TOT_MONO, UNITS, 10.5D),
                new PlanDistribution(1, DATE_IN_2, DATE_OUT_2, TOT_MONO, UNITS, 50.0D),
                new PlanDistribution(2, DATE_IN_2, DATE_OUT_2, GLOBAL, UNITS, 100.0D)
            )
        ),
        Arguments.of(
            DATE_IN_FROM,
            DATE_IN_TO,
            Set.of(NON_TOT_MONO, TOT_MONO),
            Set.of(1L, 2L),
            List.of(
                new PlanDistribution(1, DATE_IN_2, DATE_OUT_2, NON_TOT_MONO, UNITS, 10.5D),
                new PlanDistribution(1, DATE_IN_2, DATE_OUT_2, TOT_MONO, UNITS, 50.0D)
            )
        ),
        Arguments.of(
            DATE_IN_FROM,
            DATE_IN_TO,
            Set.of(),
            Set.of(1L, 2L, 3L, 4),
            List.of(
                new PlanDistribution(1, DATE_IN_2, DATE_OUT_2, GLOBAL, UNITS, 1000.0D),
                new PlanDistribution(1, DATE_IN_2, DATE_OUT_2, NON_TOT_MONO, UNITS, 10.5D),
                new PlanDistribution(1, DATE_IN_2, DATE_OUT_2, TOT_MONO, UNITS, 50.0D),
                new PlanDistribution(2, DATE_IN_2, DATE_OUT_2, GLOBAL, UNITS, 100.0D),
                new PlanDistribution(3, DATE_IN_2, DATE_OUT_2, GLOBAL, UNITS, 45.6D),
                new PlanDistribution(4, DATE_IN_2, DATE_OUT_2, GLOBAL, UNITS, 1000.0D)
            )
        )
    );
  }
}
