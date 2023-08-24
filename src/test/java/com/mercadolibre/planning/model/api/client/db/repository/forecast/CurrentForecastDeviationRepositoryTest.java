package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentForecastDeviation;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentForecastDeviationWithPath;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockListOfCurrentForecastDeviations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.Path;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CurrentForecastDeviationRepositoryTest {

  private static final Instant VIEW_DATE = Instant.parse("2022-09-08T13:00:00Z");

  private static final Instant DATE_FROM = Instant.parse("2022-09-08T12:30:00Z");

  private static final Instant DATE_TO = Instant.parse("2022-09-09T12:30:00Z");

  @Autowired
  private CurrentForecastDeviationRepository repository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("Find currentForecastDeviation active by warehouse id and workflow")
  void findByLogisticCenterIdAndWorkflowAndIsActiveAndDateInRangeOk() {
    // GIVEN
    final CurrentForecastDeviation currentForecastDeviation =
        mockCurrentForecastDeviation();
    entityManager.persist(currentForecastDeviation);
    entityManager.flush();

    // WHEN
    final List<CurrentForecastDeviation> optDeviation = repository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThan(
            WAREHOUSE_ID, FBM_WMS_OUTBOUND, DATE_IN.plusHours(1)
        );

    // THEN
    assertFalse(optDeviation.isEmpty());

    final CurrentForecastDeviation deviation = optDeviation.get(0);
    assertEquals(WAREHOUSE_ID, deviation.getLogisticCenterId());
    assertEquals(DATE_IN, deviation.getDateFrom());
    assertEquals(DATE_OUT, deviation.getDateTo());
    assertEquals(0.025, deviation.getValue());
    assertEquals("FBM_WMS_OUTBOUND", deviation.getWorkflow().name());
  }

  @Test
  @DisplayName("Find currentForecastDeviation active and unexpired by warehouse id and workflow and currentDate")
  void findByLogisticCenterIdAndWorkflowAndIsActiveAndDateToIsGreaterThanCurrentDateOk() {
    // GIVEN
    final ZonedDateTime currentDate = Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(ZoneOffset.UTC);

    // Active and expired deviations
    entityManager.persistAndFlush(
        mockCurrentForecastDeviation(
            currentDate.minus(3, ChronoUnit.HOURS),
            currentDate.minus(2, ChronoUnit.HOURS),
            true
        )
    );
    entityManager.persistAndFlush(
        mockCurrentForecastDeviation(
            currentDate.minus(3, ChronoUnit.HOURS),
            currentDate,
            true
        )
    );
    // inactive deviation
    entityManager.persistAndFlush(
        mockCurrentForecastDeviation(
            currentDate.minus(1, ChronoUnit.HOURS),
            currentDate.plus(1, ChronoUnit.HOURS),
            false
        )
    );
    // Active and unexpired deviation
    entityManager.persistAndFlush(
        mockCurrentForecastDeviation(
            currentDate,
            currentDate.plus(2, ChronoUnit.HOURS),
            true
        )
    );

    // WHEN
    final List<CurrentForecastDeviation> optDeviation = repository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThan(
            WAREHOUSE_ID, FBM_WMS_OUTBOUND, currentDate
        );

    // THEN
    assertEquals(1, optDeviation.size());

    final CurrentForecastDeviation deviation = optDeviation.get(0);
    assertTrue(deviation.getDateTo().isAfter(currentDate));
    assertTrue(deviation.isActive());

  }

  @Test
  void whenSaveNewAdjustmentWithNullPathDisableJustAdjustmentByPath() {
    // GIVEN
    final List<CurrentForecastDeviation> currentForecastDeviation = mockListOfCurrentForecastDeviations();
    currentForecastDeviation.forEach(a -> entityManager.persist(a));
    entityManager.flush();
    final CurrentForecastDeviation newEntityToPersist = CurrentForecastDeviation.builder()
        .isActive(Boolean.TRUE)
        .workflow(FBM_WMS_INBOUND)
        .logisticCenterId(WAREHOUSE_ID)
        .type(DeviationType.UNITS)
        .build();
    // WHEN
    repository.disableDeviation(WAREHOUSE_ID, FBM_WMS_INBOUND, DeviationType.UNITS, null);
    entityManager.persistAndFlush(newEntityToPersist);
    // THEN
    var current = repository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowIn(WAREHOUSE_ID, Set.of(FBM_WMS_INBOUND));
    for (int i = 0; i < 2; i++) {
      assertEquals(current.get(i).getType(), currentForecastDeviation.get(i).getType());
      assertEquals(current.get(i).getLogisticCenterId(), currentForecastDeviation.get(i).getLogisticCenterId());
      assertEquals(current.get(i).getWorkflow(), currentForecastDeviation.get(i).getWorkflow());
      assertEquals(current.get(i).getPath(), currentForecastDeviation.get(i).getPath());
    }
  }

  @Test
  @DisplayName("Find currentForecastDeviation when no deviation exists for a warehouseId")
  void findByLogisticCenterIdAndWorkflowAndIsActiveWhenNotExistDeviation() {
    // WHEN
    final List<CurrentForecastDeviation> optDeviation = repository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThan(
            WAREHOUSE_ID, FBM_WMS_OUTBOUND, A_DATE_UTC
        );

    // THEN
    assertTrue(optDeviation.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ARTW02", "ARTW03", "ARTW04"})
  @Sql("/sql/forecast/load_forecast_and_metadata.sql")
  void testSearchDeviationShouldReturnExpectedValue(final String logisticCenterId) {
    // WHEN
    final var result = repository.findActiveDeviationAt(
        logisticCenterId,
        FBM_WMS_OUTBOUND.name(),
        VIEW_DATE
    );

    // THEN
    assertEquals(1, result.size());

    final var deviation = result.get(0);
    assertEquals(0.5, deviation.getValue());
    assertEquals(DATE_FROM, deviation.getDateFrom().toInstant());
    assertEquals(DATE_TO, deviation.getDateTo().toInstant());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ARTW00", "ARTW01"})
  @Sql("/sql/forecast/load_forecast_and_metadata.sql")
  void testSearchDeviationsShouldNotProduceAnyResult(final String logisticCenterId) {
    // WHEN
    final var result = repository.findActiveDeviationAt(
        logisticCenterId,
        FBM_WMS_OUTBOUND.name(),
        VIEW_DATE
    );

    // THEN
    assertTrue(result.isEmpty());
  }

  @Test
  void testFindCurrentActiveDeviationsWithoutPath() {

    final ZonedDateTime currentDate = Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(ZoneOffset.UTC);

    mockEntitiesToPersistWithoutPath(currentDate);


    final List<CurrentForecastDeviation> currentForecastDeviations =
        repository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID,
            Set.of(FBM_WMS_OUTBOUND),
            null,
            DeviationType.UNITS,
            currentDate
        );

    assertEquals(1, currentForecastDeviations.size());

    final CurrentForecastDeviation deviation = currentForecastDeviations.get(0);
    assertTrue(deviation.getDateTo().isAfter(currentDate));
    assertTrue(deviation.isActive());
  }

  @Test
  void testFindCurrentActiveDeviationsWithPath() {

    final ZonedDateTime currentDate = Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(ZoneOffset.UTC);

    mockPathEntitiesToPersistWithPath(currentDate);
    final Set<Path> paths = Set.of(Path.FTL, Path.COLLECT, Path.SPD);

    final List<CurrentForecastDeviation> deviations = paths.stream()
        .map(path -> repository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowInAndPathAndTypeAndDateToIsGreaterThan(
            WAREHOUSE_ID,
            Set.of(INBOUND),
            path,
            DeviationType.MINUTES,
            currentDate))
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    assertEquals(2, deviations.size());
    assertEquals(2, deviations.stream()
        .filter(deviation -> deviation.getDateTo().isAfter(currentDate)).count());
    assertEquals(1, deviations.stream()
        .filter(deviation -> deviation.getPath().equals(Path.FTL) && deviation.isActive()).count());
    assertEquals(1, deviations.stream()
        .filter(deviation -> deviation.getPath().equals(Path.COLLECT) && deviation.isActive()).count());
    assertEquals(0, deviations.stream()
        .filter(deviation -> deviation.getPath().equals(Path.SPD)).count());
  }

  private void mockEntitiesToPersistWithoutPath(final ZonedDateTime currentDate) {
    entityManager.persistAndFlush(
        mockCurrentForecastDeviationWithPath(
            FBM_WMS_OUTBOUND,
            null,
            DeviationType.UNITS,
            true,
            0.1,
            currentDate.minus(12, ChronoUnit.HOURS),
            currentDate.minus(8, ChronoUnit.HOURS)
        )

    );

    entityManager.persistAndFlush(
        mockCurrentForecastDeviationWithPath(
            FBM_WMS_OUTBOUND,
            null,
            DeviationType.UNITS,
            true,
            0.1,
            currentDate.minus(3, ChronoUnit.HOURS),
            currentDate.plus(3, ChronoUnit.HOURS)
        )
    );

  }

  private void mockPathEntitiesToPersistWithPath(final ZonedDateTime currentDate) {

    entityManager.persistAndFlush(
        mockCurrentForecastDeviationWithPath(
            INBOUND,
            Path.COLLECT,
            DeviationType.MINUTES,
            true,
            80,
            currentDate.minus(5, ChronoUnit.HOURS),
            currentDate.minus(3, ChronoUnit.HOURS)
        )

    );

    entityManager.persistAndFlush(
        mockCurrentForecastDeviationWithPath(
            INBOUND,
            Path.SPD,
            DeviationType.MINUTES,
            true,
            80,
            currentDate.minus(8, ChronoUnit.HOURS),
            currentDate.minus(4, ChronoUnit.HOURS)
        )

    );

    entityManager.persistAndFlush(
        mockCurrentForecastDeviationWithPath(
            INBOUND,
            Path.FTL,
            DeviationType.MINUTES,
            true,
            80,
            currentDate.minus(12, ChronoUnit.HOURS),
            currentDate.minus(8, ChronoUnit.HOURS)
        )

    );

    entityManager.persistAndFlush(
        mockCurrentForecastDeviationWithPath(
            INBOUND,
            Path.FTL,
            DeviationType.MINUTES,
            true,
            30,
            currentDate.minus(3, ChronoUnit.HOURS),
            currentDate.plus(3, ChronoUnit.HOURS)
        )
    );

    entityManager.persistAndFlush(
        mockCurrentForecastDeviationWithPath(
            INBOUND,
            Path.COLLECT,
            DeviationType.MINUTES,
            true,
            30,
            currentDate.minus(3, ChronoUnit.HOURS),
            currentDate.plus(3, ChronoUnit.HOURS)
        )
    );
  }
}
