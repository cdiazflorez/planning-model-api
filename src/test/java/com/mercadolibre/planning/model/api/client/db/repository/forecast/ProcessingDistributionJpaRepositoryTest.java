package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE;
import static com.mercadolibre.planning.model.api.util.TestUtils.HEADCOUNT_TYPE;
import static com.mercadolibre.planning.model.api.util.TestUtils.MAIN;
import static com.mercadolibre.planning.model.api.util.TestUtils.NON_SYSTEMIC;
import static com.mercadolibre.planning.model.api.util.TestUtils.POLYVALENCE;
import static com.mercadolibre.planning.model.api.util.TestUtils.POLYVALENT;
import static com.mercadolibre.planning.model.api.util.TestUtils.PROCESS_NAME;
import static com.mercadolibre.planning.model.api.util.TestUtils.PROCESS_PATH;
import static com.mercadolibre.planning.model.api.util.TestUtils.SYSTEMIC;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static com.mercadolibre.planning.model.api.util.TestUtils.objectMapper;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.fill;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanInput;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProcessingDistributionJpaRepositoryTest {

  private static final ZonedDateTime DATE_TIME = ZonedDateTime.now().truncatedTo(HOURS);

  @Autowired
  private EntityManager entityManager;

  @Test
  void testCreate() {
    // GIVEN
    final Forecast forecast = mockSimpleForecast();
    entityManager.persist(forecast);

    final ProcessingDistributionJpaRepository repository = new ProcessingDistributionJpaRepository(entityManager);

    final ProcessingDistribution entity = ProcessingDistribution.builder()
        .date(DATE_TIME)
        .processPath(GLOBAL)
        .processName(PICKING)
        .quantity(10)
        .quantityMetricUnit(UNITS)
        .type(EFFECTIVE_WORKERS)
        .build();

    final ProcessingDistribution[] entities = new ProcessingDistribution[950];
    fill(entities, entity);

    //WHEN
    repository.create(Arrays.asList(entities), forecast.getId());

    //THEN
    final Query query = entityManager.createNativeQuery("select * from processing_distribution", ProcessingDistribution.class);

    final List<ProcessingDistribution> persistedEntities = query.getResultList();
    assertEquals(950, persistedEntities.size());

    assertEquals(entity.getDate(), persistedEntities.get(0).getDate());
    assertEquals(entity.getProcessPath(), persistedEntities.get(0).getProcessPath());
    assertEquals(entity.getProcessName(), persistedEntities.get(0).getProcessName());
    assertEquals(entity.getQuantity(), persistedEntities.get(0).getQuantity());
    assertEquals(entity.getQuantityMetricUnit(), persistedEntities.get(0).getQuantityMetricUnit());
  }

  @Test
  void testCreateWithTags() {
    // GIVEN
    final Forecast forecast = mockSimpleForecast();
    entityManager.persist(forecast);

    final ProcessingDistributionJpaRepository repository = new ProcessingDistributionJpaRepository(entityManager);

    final ProcessingDistribution entity = ProcessingDistribution.builder()
        .date(DATE_TIME)
        .processPath(ProcessPath.GLOBAL)
        .processName(ProcessName.PICKING)
        .quantity(10)
        .quantityMetricUnit(UNITS)
        .type(EFFECTIVE_WORKERS)
        .tags("{\"process_name\": \"PICKING\", \"process_path\": \"TOT_SINGLE_SKU\", \"headcount_type\": \"systemic\"}")
        .build();

    repository.create(List.of(entity), forecast.getId());


    final Query query = entityManager.createNativeQuery(
        "select * from processing_distribution",
        ProcessingDistribution.class
    );

    final List<ProcessingDistribution> persistedEntities = query.getResultList();
    assertEquals(1, persistedEntities.size());
    assertEquals(entity.getTags(), persistedEntities.get(0).getTags());
  }

  @ParameterizedTest
  @MethodSource("provideArgumentsToGetStaffingPlan")
  void testGetStaffingPlan(final ProcessingType type,
                           final List<String> groupers,
                           final Map<String, List<Object>> filters,
                           final List<StaffingPlan> expectedResponse) {
    // GIVEN
    final Forecast firstForecast = mockSimpleForecast();
    entityManager.persist(firstForecast);

    final List<ProcessingDistribution> firstProcessingDistribution = mockProcessingDistributions(firstForecast);
    firstProcessingDistribution.forEach(
        processingDistribution -> entityManager.persist(processingDistribution)
    );

    final Forecast secondForecast = mockSimpleForecast();
    entityManager.persist(secondForecast);

    final List<ProcessingDistribution> secondProcessingDistribution = List.of(
        //HEADCOUNT
        buildProcessingDistribution(secondForecast, PACKING, GLOBAL, SYSTEMIC, EFFECTIVE_WORKERS, null, 20D),
        buildProcessingDistribution(secondForecast, PACKING, GLOBAL, NON_SYSTEMIC, EFFECTIVE_WORKERS_NS, null, 10D),
        //PRODUCTIVITY
        buildProcessingDistribution(secondForecast, PACKING, GLOBAL, null, PRODUCTIVITY, MAIN, 200D),
        buildProcessingDistribution(secondForecast, PACKING, GLOBAL, null, PRODUCTIVITY, POLYVALENT, 180D),
        //THROUGHPUT
        buildProcessingDistribution(secondForecast, PACKING, GLOBAL, null, THROUGHPUT, null, 4000D),
        //MAX_CAPACITY
        buildProcessingDistribution(secondForecast, ProcessName.GLOBAL, GLOBAL, null, MAX_CAPACITY, null, 1500D)
    );

    secondProcessingDistribution.forEach(
        processingDistribution -> entityManager.persist(processingDistribution)
    );

    final ProcessingDistributionJpaRepository repository = new ProcessingDistributionJpaRepository(entityManager);

    // WHEN
    final var result = repository.getStaffingPlan(
        new StaffingPlanInput(
            List.of(firstForecast.getId(), secondForecast.getId()),
            DATE_TIME.toInstant(),
            DATE_TIME.toInstant(),
            type,
            groupers,
            filters
        )
    );
    // THEN
    assertFalse(result.isEmpty());
    assertEquals(expectedResponse.size(), result.size());
    assertTrue(expectedResponse.containsAll(result) && result.containsAll(expectedResponse));
  }

  private static Stream<Arguments> provideArgumentsToGetStaffingPlan() {
    return Stream.of(
        Arguments.of(
            EFFECTIVE_WORKERS,
            List.of(),
            Map.of(
                PROCESS_NAME, List.of(PACKING.getName())
            ),
            List.of(
                new StaffingPlan(20D, UNITS, EFFECTIVE_WORKERS, emptyMap())
            )
        ),
        Arguments.of(
            EFFECTIVE_WORKERS,
            List.of(),
            Map.of(
                PROCESS_NAME, List.of(PICKING.getName()),
                PROCESS_PATH, List.of(GLOBAL.toJson())
            ),
            List.of(
                new StaffingPlan(10D, UNITS, EFFECTIVE_WORKERS, emptyMap())
            )
        ),
        Arguments.of(
            EFFECTIVE_WORKERS,
            List.of(DATE, PROCESS_NAME, PROCESS_PATH, HEADCOUNT_TYPE),
            Map.of(
                PROCESS_NAME, List.of(PICKING.getName()),
                HEADCOUNT_TYPE, List.of(SYSTEMIC)
            ),
            List.of(
                new StaffingPlan(10D,
                                 UNITS,
                                 EFFECTIVE_WORKERS,
                                 Map.of(
                                     DATE, DATE_TIME.toInstant().toString(),
                                     PROCESS_NAME, PICKING.getName(),
                                     PROCESS_PATH, GLOBAL.toJson(),
                                     HEADCOUNT_TYPE, SYSTEMIC
                                 )
                ),
                new StaffingPlan(2D,
                                 UNITS,
                                 EFFECTIVE_WORKERS,
                                 Map.of(
                                     DATE, DATE_TIME.toInstant().toString(),
                                     PROCESS_NAME, PICKING.getName(),
                                     PROCESS_PATH, TOT_MULTI_BATCH.toJson(),
                                     HEADCOUNT_TYPE, SYSTEMIC
                                 )
                ),
                new StaffingPlan(2D,
                                 UNITS,
                                 EFFECTIVE_WORKERS,
                                 Map.of(
                                     DATE, DATE_TIME.toInstant().toString(),
                                     PROCESS_NAME, PICKING.getName(),
                                     PROCESS_PATH, NON_TOT_MULTI_BATCH.toJson(),
                                     HEADCOUNT_TYPE, SYSTEMIC
                                 )
                ),
                new StaffingPlan(2D,
                                 UNITS,
                                 EFFECTIVE_WORKERS,
                                 Map.of(
                                     DATE, DATE_TIME.toInstant().toString(),
                                     PROCESS_NAME, PICKING.getName(),
                                     PROCESS_PATH, TOT_MONO.toJson(),
                                     HEADCOUNT_TYPE, SYSTEMIC
                                 )
                ),
                new StaffingPlan(2D,
                                 UNITS,
                                 EFFECTIVE_WORKERS,
                                 Map.of(
                                     DATE, DATE_TIME.toInstant().toString(),
                                     PROCESS_NAME, PICKING.getName(),
                                     PROCESS_PATH, NON_TOT_MONO.toJson(),
                                     HEADCOUNT_TYPE, SYSTEMIC
                                 )
                ),
                new StaffingPlan(2D,
                                 UNITS,
                                 EFFECTIVE_WORKERS,
                                 Map.of(
                                     DATE, DATE_TIME.toInstant().toString(),
                                     PROCESS_NAME, PICKING.getName(),
                                     PROCESS_PATH, TOT_MULTI_ORDER.toJson(),
                                     HEADCOUNT_TYPE, SYSTEMIC
                                 )
                )
            )
        ),
        Arguments.of(
            PRODUCTIVITY,
            List.of(),
            Map.of(
                PROCESS_PATH, List.of(GLOBAL.toJson()),
                PROCESS_NAME, List.of(PICKING.getName()),
                POLYVALENCE, List.of(POLYVALENT)
            ),
            List.of(
                new StaffingPlan(80D, UNITS, PRODUCTIVITY, emptyMap())
            )
        ),
        Arguments.of(
            THROUGHPUT,
            List.of(DATE),
            Map.of(
                PROCESS_PATH, List.of(GLOBAL.toJson()),
                PROCESS_NAME, List.of(PICKING.toJson())
            ),
            List.of(
                new StaffingPlan(
                    1000D,
                    UNITS,
                    THROUGHPUT,
                    Map.of(
                        DATE, DATE_TIME.toInstant().toString()
                    )
                )
            )
        ),
        Arguments.of(
            MAX_CAPACITY,
            List.of(DATE),
            Map.of(),
            List.of(
                new StaffingPlan(
                    1500D,
                    UNITS,
                    MAX_CAPACITY,
                    Map.of(
                        DATE, DATE_TIME.toInstant().toString()
                    )
                )
            )
        )
    );
  }

  private static List<ProcessingDistribution> mockProcessingDistributions(final Forecast forecast) {
    return List.of(
        //HEADCOUNT
        buildProcessingDistribution(forecast, PICKING, GLOBAL, SYSTEMIC, EFFECTIVE_WORKERS, null, 10D),
        buildProcessingDistribution(forecast, PICKING, TOT_MULTI_BATCH, SYSTEMIC, EFFECTIVE_WORKERS, null, 2D),
        buildProcessingDistribution(forecast, PICKING, NON_TOT_MULTI_BATCH, SYSTEMIC, EFFECTIVE_WORKERS, null, 2D),
        buildProcessingDistribution(forecast, PICKING, TOT_MONO, SYSTEMIC, EFFECTIVE_WORKERS, null, 2D),
        buildProcessingDistribution(forecast, PICKING, NON_TOT_MONO, SYSTEMIC, EFFECTIVE_WORKERS, null, 2D),
        buildProcessingDistribution(forecast, PICKING, TOT_MULTI_ORDER, SYSTEMIC, EFFECTIVE_WORKERS, null, 2D),
        buildProcessingDistribution(forecast, PICKING, GLOBAL, NON_SYSTEMIC, EFFECTIVE_WORKERS_NS, null, 5D),
        buildProcessingDistribution(forecast, PACKING, GLOBAL, SYSTEMIC, EFFECTIVE_WORKERS, null, 15D),
        buildProcessingDistribution(forecast, PACKING, GLOBAL, NON_SYSTEMIC, EFFECTIVE_WORKERS_NS, null, 5D),
        //PRODUCTIVITY
        buildProcessingDistribution(forecast, PICKING, GLOBAL, null, PRODUCTIVITY, MAIN, 100D),
        buildProcessingDistribution(forecast, PICKING, GLOBAL, null, PRODUCTIVITY, POLYVALENT, 80D),
        buildProcessingDistribution(forecast, PICKING, TOT_MULTI_BATCH, null, PRODUCTIVITY, MAIN, 20D),
        buildProcessingDistribution(forecast, PICKING, TOT_MULTI_BATCH, null, PRODUCTIVITY, POLYVALENT, 16D),
        buildProcessingDistribution(forecast, PICKING, NON_TOT_MULTI_BATCH, null, PRODUCTIVITY, MAIN, 20D),
        buildProcessingDistribution(forecast, PICKING, NON_TOT_MULTI_BATCH, null, PRODUCTIVITY, POLYVALENT, 16D),
        buildProcessingDistribution(forecast, PICKING, TOT_MONO, null, PRODUCTIVITY, MAIN, 20D),
        buildProcessingDistribution(forecast, PICKING, TOT_MONO, null, PRODUCTIVITY, POLYVALENT, 16D),
        buildProcessingDistribution(forecast, PICKING, NON_TOT_MONO, null, PRODUCTIVITY, MAIN, 20D),
        buildProcessingDistribution(forecast, PICKING, NON_TOT_MONO, null, PRODUCTIVITY, POLYVALENT, 16D),
        buildProcessingDistribution(forecast, PICKING, TOT_MULTI_ORDER, null, PRODUCTIVITY, MAIN, 20D),
        buildProcessingDistribution(forecast, PICKING, TOT_MULTI_ORDER, null, PRODUCTIVITY, POLYVALENT, 16D),
        buildProcessingDistribution(forecast, PACKING, GLOBAL, null, PRODUCTIVITY, MAIN, 150D),
        buildProcessingDistribution(forecast, PACKING, GLOBAL, null, PRODUCTIVITY, POLYVALENT, 120D),
        //THROUGHPUT
        buildProcessingDistribution(forecast, PICKING, GLOBAL, null, THROUGHPUT, null, 1000D),
        buildProcessingDistribution(forecast, PICKING, TOT_MULTI_BATCH, null, THROUGHPUT, null, 200D),
        buildProcessingDistribution(forecast, PICKING, NON_TOT_MULTI_BATCH, null, THROUGHPUT, null, 200D),
        buildProcessingDistribution(forecast, PICKING, TOT_MONO, null, THROUGHPUT, null, 200D),
        buildProcessingDistribution(forecast, PICKING, NON_TOT_MONO, null, THROUGHPUT, null, 200D),
        buildProcessingDistribution(forecast, PICKING, TOT_MULTI_ORDER, null, THROUGHPUT, null, 200D),
        buildProcessingDistribution(forecast, PACKING, GLOBAL, null, THROUGHPUT, null, 1500D),
        //MAX_CAPACITY
        buildProcessingDistribution(forecast, ProcessName.GLOBAL, GLOBAL, null, MAX_CAPACITY, null, 500D)
    );
  }

  private static ProcessingDistribution buildProcessingDistribution(final Forecast forecast,
                                                                    final ProcessName processName,
                                                                    final ProcessPath processPath,
                                                                    final String headcountType,
                                                                    final ProcessingType processingType,
                                                                    final Integer polyvalence,
                                                                    final double quantity) {
    return ProcessingDistribution.builder()
        .forecast(forecast)
        .date(DATE_TIME)
        .processPath(processPath)
        .processName(processName)
        .quantity(quantity)
        .quantityMetricUnit(UNITS)
        .type(processingType)
        .tags(processingType == MAX_CAPACITY
                  ? "{}" : buildTags(processName, processPath, headcountType, polyvalence))
        .build();
  }

  private static String buildTags(final ProcessName processName,
                                  final ProcessPath processPath,
                                  final String headcountType,
                                  final Integer polyvalence) {
    final ConcurrentHashMap<String, Object> tags = new ConcurrentHashMap<>();

    if (processName != null) {
      tags.put(PROCESS_NAME, processName.getName());
    }

    if (processPath != null) {
      tags.put(PROCESS_PATH, processPath.toJson());
    }

    if (headcountType != null) {
      tags.put(HEADCOUNT_TYPE, headcountType);
    }

    if (polyvalence != null) {
      tags.put(POLYVALENCE, polyvalence);
    }

    try {
      return objectMapper().writeValueAsString(tags);
    } catch (JsonProcessingException e) {
      return "";
    }
  }
}
