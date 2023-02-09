package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentForecastDeviation;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockListOfCurrentForecastDeviations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
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
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
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
}
