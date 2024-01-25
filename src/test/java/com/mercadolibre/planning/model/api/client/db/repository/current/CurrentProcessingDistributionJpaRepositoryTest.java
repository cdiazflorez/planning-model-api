package com.mercadolibre.planning.model.api.client.db.repository.current;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_SINGLE_SKU;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_SINGLE_SKU;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE;
import static com.mercadolibre.planning.model.api.util.TestUtils.HEADCOUNT_TYPE;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.NON_SYSTEMIC;
import static com.mercadolibre.planning.model.api.util.TestUtils.POLYVALENCE;
import static com.mercadolibre.planning.model.api.util.TestUtils.PROCESS_NAME;
import static com.mercadolibre.planning.model.api.util.TestUtils.PROCESS_PATH;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.plan.CurrentStaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CurrentProcessingDistributionJpaRepositoryTest {

  private static final Instant DATE_FROM = Instant.parse("2024-01-22T18:00:00Z");

  private static final Instant DATE_TO = DATE_FROM.plus(1, HOURS);

  @Autowired
  private EntityManager entityManager;

  @Sql(scripts = "/sql/current/load_current_processing_distribution.sql")
  @ParameterizedTest
  @MethodSource("provideCurrentStaffingPlanInputAndExpectedResult")
  void testGetCurrentStaffingPlan(final CurrentStaffingPlanInput input,
                                  final Set<StaffingPlan> expectedResult) {
    // GIVEN
    final CurrentProcessingDistributionJpaRepository repository = new CurrentProcessingDistributionJpaRepository(entityManager);
    // WHEN
    final List<StaffingPlan> result = repository.getCurrentStaffingPlan(input);
    // THEN
    assertEquals(expectedResult.size(), result.size());
    assertEquals(expectedResult, new HashSet<>(result));
  }

  private static Stream<Arguments> provideCurrentStaffingPlanInputAndExpectedResult() {
    return Stream.of(
        Arguments.of(
            new CurrentStaffingPlanInput(
                LOGISTIC_CENTER_ID,
                FBM_WMS_OUTBOUND,
                DATE_FROM,
                DATE_TO,
                EFFECTIVE_WORKERS,
                emptyList(),
                Map.of(
                    PROCESS_NAME, List.of(PICKING.getName())
                )
            ),
            Set.of(
                new StaffingPlan(
                    10D,
                    WORKERS,
                    EFFECTIVE_WORKERS,
                    emptyMap()
                )
            )
        ),
        Arguments.of(
            new CurrentStaffingPlanInput(
                LOGISTIC_CENTER_ID,
                FBM_WMS_OUTBOUND,
                DATE_FROM,
                DATE_TO,
                EFFECTIVE_WORKERS,
                List.of(DATE, PROCESS_NAME),
                emptyMap()
            ),
            Set.of(
                new StaffingPlan(
                    10D,
                    WORKERS,
                    EFFECTIVE_WORKERS,
                    Map.of(
                        DATE, DATE_FROM.toString(),
                        PROCESS_NAME, PICKING.getName()
                    )
                ),
                new StaffingPlan(
                    12D,
                    WORKERS,
                    EFFECTIVE_WORKERS,
                    Map.of(
                        DATE, DATE_FROM.toString(),
                        PROCESS_NAME, PACKING.getName()
                    )
                )
            )
        ),
        Arguments.of(
            new CurrentStaffingPlanInput(
                LOGISTIC_CENTER_ID,
                FBM_WMS_OUTBOUND,
                DATE_FROM,
                DATE_TO,
                EFFECTIVE_WORKERS_NS,
                List.of(DATE, PROCESS_NAME, HEADCOUNT_TYPE),
                emptyMap()
            ),
            Set.of(
                new StaffingPlan(
                    5D,
                    WORKERS,
                    EFFECTIVE_WORKERS_NS,
                    Map.of(
                        DATE, DATE_FROM.toString(),
                        PROCESS_NAME, PICKING.getName(),
                        HEADCOUNT_TYPE, NON_SYSTEMIC
                    )
                ),
                new StaffingPlan(
                    5D,
                    WORKERS,
                    EFFECTIVE_WORKERS_NS,
                    Map.of(
                        DATE, DATE_FROM.toString(),
                        PROCESS_NAME, PACKING.getName(),
                        HEADCOUNT_TYPE, NON_SYSTEMIC
                    )
                )
            )
        ),
        Arguments.of(
            new CurrentStaffingPlanInput(
                LOGISTIC_CENTER_ID,
                FBM_WMS_OUTBOUND,
                DATE_FROM,
                DATE_TO,
                PRODUCTIVITY,
                List.of(DATE, PROCESS_NAME, PROCESS_PATH),
                Map.of(
                    PROCESS_NAME, List.of(PICKING.getName()),
                    PROCESS_PATH, List.of(NON_TOT_SINGLE_SKU.toJson())
                )
            ),
            Set.of(
                new StaffingPlan(
                    45D,
                    UNITS_PER_HOUR,
                    PRODUCTIVITY,
                    Map.of(
                        DATE, DATE_FROM.toString(),
                        PROCESS_NAME, PICKING.getName(),
                        PROCESS_PATH, NON_TOT_SINGLE_SKU.toJson()
                    )
                )
            )
        ),
        Arguments.of(
            new CurrentStaffingPlanInput(
                LOGISTIC_CENTER_ID,
                FBM_WMS_OUTBOUND,
                DATE_FROM,
                DATE_TO,
                PRODUCTIVITY,
                emptyList(),
                Map.of(
                    PROCESS_NAME, List.of(PICKING.getName()),
                    PROCESS_PATH, List.of(TOT_MULTI_BATCH.toJson()),
                    POLYVALENCE, List.of(2)
                )
            ),
            Set.of(
                new StaffingPlan(
                    77D,
                    UNITS_PER_HOUR,
                    PRODUCTIVITY,
                    emptyMap()
                )
            )
        ),
        Arguments.of(
            new CurrentStaffingPlanInput(
                LOGISTIC_CENTER_ID,
                FBM_WMS_OUTBOUND,
                DATE_FROM,
                DATE_TO,
                THROUGHPUT,
                List.of(DATE),
                Map.of(
                    PROCESS_NAME, List.of(PICKING.getName()),
                    PROCESS_PATH, List.of(TOT_SINGLE_SKU.toJson())
                )
            ),
            Set.of(
                new StaffingPlan(
                    1750D,
                    UNITS_PER_HOUR,
                    THROUGHPUT,
                    Map.of(
                        DATE, DATE_FROM.toString()
                    )
                )
            )
        ),
        Arguments.of(
            new CurrentStaffingPlanInput(
                LOGISTIC_CENTER_ID,
                FBM_WMS_OUTBOUND,
                DATE_FROM,
                DATE_TO,
                MAX_CAPACITY,
                List.of(DATE),
                emptyMap()
            ),
            Set.of(
                new StaffingPlan(
                    5600D,
                    UNITS_PER_HOUR,
                    MAX_CAPACITY,
                    Map.of(
                        DATE, DATE_FROM.toString()
                    )
                ),
                new StaffingPlan(
                    4400D,
                    UNITS_PER_HOUR,
                    MAX_CAPACITY,
                    Map.of(
                        DATE, DATE_TO.toString()
                    )
                )
            )
        ),
        Arguments.of(
            new CurrentStaffingPlanInput(
                LOGISTIC_CENTER_ID,
                FBM_WMS_OUTBOUND,
                DATE_FROM,
                DATE_TO,
                MAX_CAPACITY,
                emptyList(),
                emptyMap()
            ),
            Set.of(
                new StaffingPlan(
                    10000D,
                    UNITS_PER_HOUR,
                    MAX_CAPACITY,
                    emptyMap()
                )
            )
        ),
        Arguments.of(
            new CurrentStaffingPlanInput(
                LOGISTIC_CENTER_ID,
                FBM_WMS_OUTBOUND,
                DATE_FROM,
                DATE_TO,
                EFFECTIVE_WORKERS,
                emptyList(),
                Map.of(
                    "Invalid", List.of("Invalid")
                )
            ),
            emptySet()
        )
    );
  }

}
