package com.mercadolibre.planning.model.api.client.db.repository.current;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DEACTIVATE_DATE_FROM;
import static com.mercadolibre.planning.model.api.util.TestUtils.DEACTIVATE_DATE_TO;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProdEntity;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProductivities;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
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
public class CurrentHeadcountProductivityRepositoryTest {

  public static final ZonedDateTime DATE_FROM = ZonedDateTime.parse("2022-09-08T12:00:00Z");

  public static final ZonedDateTime DATE_TO = ZonedDateTime.parse("2022-09-08T14:00:00Z");

  public static final Instant VIEW_DATE = Instant.parse("2022-09-08T10:30:00Z");

  @Autowired
  private CurrentHeadcountProductivityRepository repository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("Looking for a current headcount productivity that exists, returns it")
  public void testFindCurrentHeadcountProductivityById() {
    // GIVEN
    final CurrentHeadcountProductivity currentProd = mockCurrentProdEntity(A_DATE_UTC, 68L);
    entityManager.persistAndFlush(currentProd);

    // WHEN
    final Optional<CurrentHeadcountProductivity> optCurrentProd = repository.findById(1L);

    // THEN
    assertTrue(optCurrentProd.isPresent());

    final CurrentHeadcountProductivity foundCurrentProd = optCurrentProd.get();
    assertCurrentProductivity(currentProd, foundCurrentProd);
  }

  @Test
  @DisplayName("Looking for a current headcount productivity that doesn't exist, returns nothing")
  public void testCurrentHeadcountProductivityDoesntExist() {
    // WHEN
    final Optional<CurrentHeadcountProductivity> optProd = repository.findById(1L);

    // THEN
    assertFalse(optProd.isPresent());
  }

  @Test
  @DisplayName("Deactivate productivity")
  public void testDeactivateProductivity() {
    // GIVEN
    final CurrentHeadcountProductivity currentProd = mockCurrentProdEntity(A_DATE_UTC, 68L);
    entityManager.persistAndFlush(currentProd);

    // WHEN
    repository.deactivateProductivity(WAREHOUSE_ID, FBM_WMS_OUTBOUND, PICKING,
        singletonList(A_DATE_UTC), UNITS_PER_HOUR, USER_ID, 1);

    final Optional<CurrentHeadcountProductivity> result = repository.findById(1L);

    // THEN
    assertTrue(result.isPresent());
    assertFalse(result.get().isActive());
    assertEquals(USER_ID, result.get().getUserId());
  }

  @Test
  @DisplayName("Deactivate productivity for range of dates")
  public void testDeactivateProductivityForRangeOfDates() {
    //GIVEN
    final List<CurrentHeadcountProductivity> currentHeadcountProductivities = mockCurrentProductivities();
    currentHeadcountProductivities.forEach(
        currentHeadcountProductivity -> entityManager.persistAndFlush(currentHeadcountProductivity)
    );

    //WHEN
    repository.deactivateProductivityForRangeOfDates(WAREHOUSE_ID, FBM_WMS_OUTBOUND, DEACTIVATE_DATE_FROM, DEACTIVATE_DATE_TO, USER_ID);

    //THEN
    final List<CurrentHeadcountProductivity> results = repository.findAll();
    assertFalse(results.isEmpty());
    assertEquals(3,
        results.stream()
            .filter(CurrentHeadcountProductivity::isActive)
            .count()
    );
    assertEquals(1,
        results.stream()
            .filter(currentHeadcountProductivity -> !currentHeadcountProductivity.isActive())
            .count()
    );

  }

  @ParameterizedTest
  @ValueSource(strings = {"ARTW00", "ARTW01", "ARTW02"})
  @Sql("/sql/forecast/load_forecast_and_metadata.sql")
  void testCurrentHeadcountProductivityWithViewDateShouldProduceNoResults(final String logisticCenterId) {
    // GIVEN

    // WHEN
    final var result = repository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
        logisticCenterId,
        FBM_WMS_OUTBOUND.name(),
        Set.of(PICKING.name(), PACKING.name()),
        DATE_FROM,
        DATE_TO,
        VIEW_DATE
    );

    // THEN
    assertTrue(result.isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ARTW03", "ARTW04"})
  @Sql("/sql/forecast/load_forecast_and_metadata.sql")
  void testCurrentHeadcountProductivityWithViewDateShouldFoundResults(final String logisticCenterId) {
    // GIVEN

    // WHEN
    final var result = repository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRangeAtViewDate(
        logisticCenterId,
        FBM_WMS_OUTBOUND.name(),
        Set.of(PICKING.name(), PACKING.name()),
        DATE_FROM,
        DATE_TO,
        VIEW_DATE
    );

    // THEN
    assertEquals(2, result.size());

    final var picking = result.stream()
        .filter(p -> p.getProcessName() == PICKING)
        .findFirst();

    assertTrue(picking.isPresent());
    assertEquals(10L, picking.get().getProductivity());


    final var packing = result.stream()
        .filter(p -> p.getProcessName() == PACKING)
        .findFirst();

    assertTrue(packing.isPresent());
    assertEquals(10L, packing.get().getProductivity());
  }

  private void assertCurrentProductivity(final CurrentHeadcountProductivity currentProd,
                                         final CurrentHeadcountProductivity foundCurrentProd) {
    assertEquals(currentProd.getId(), foundCurrentProd.getId());
    assertEquals(currentProd.getWorkflow(), foundCurrentProd.getWorkflow());
    assertEquals(currentProd.getAbilityLevel(), foundCurrentProd.getAbilityLevel());
    assertEquals(currentProd.getLogisticCenterId(), foundCurrentProd.getLogisticCenterId());
    assertEquals(currentProd.getDate(), foundCurrentProd.getDate());
    assertEquals(currentProd.getProcessName(), foundCurrentProd.getProcessName());
    assertEquals(currentProd.getProductivity(), foundCurrentProd.getProductivity());
    assertEquals(currentProd.getProductivityMetricUnit(),
        foundCurrentProd.getProductivityMetricUnit());
    assertEquals(currentProd.isActive(), foundCurrentProd.isActive());
  }
}
