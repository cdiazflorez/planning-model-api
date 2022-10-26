package com.mercadolibre.planning.model.api.client.db.repository.current;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DEACTIVATE_DATE_FROM;
import static com.mercadolibre.planning.model.api.util.TestUtils.DEACTIVATE_DATE_TO;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProcDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProcessingDistributions;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
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
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@ActiveProfiles("development")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CurrentProcessingDistributionRepositoryTest {

  public static final ZonedDateTime DATE_FROM = ZonedDateTime.parse("2022-09-08T12:00:00Z");

  public static final ZonedDateTime DATE_TO = ZonedDateTime.parse("2022-09-08T14:00:00Z");

  public static final Instant VIEW_DATE = Instant.parse("2022-09-08T10:30:00Z");

  @Autowired
  private CurrentProcessingDistributionRepository repository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("Looking for a current processing distribution that exists, returns it")
  void testFindCurrentProcessingDistributionById() {
    // GIVEN
    final CurrentProcessingDistribution currentProcessingDist = mockCurrentProcDist(
        A_DATE_UTC, 35L);

    entityManager.persistAndFlush(currentProcessingDist);

    // WHEN
    final Optional<CurrentProcessingDistribution> optCurrentProcessingDist =
        repository.findById(1L);

    // THEN
    assertTrue(optCurrentProcessingDist.isPresent());

    final CurrentProcessingDistribution foundCurrentProc = optCurrentProcessingDist.get();
    whenCurrentDistributionIsOk(currentProcessingDist, foundCurrentProc);
  }

  @Test
  @DisplayName("Looking for a current processing distribution that doesn't exist, returns nothing")
  void testCurrentProcessingDistributionDoesntExist() {
    // WHEN
    final Optional<CurrentProcessingDistribution> optDistribution = repository
        .findById(1L);

    // THEN
    assertFalse(optDistribution.isPresent());
  }

  @Test
  @DisplayName("Looking for a current processing distributions that exists filterin by different params, returns it")
  void testFindCurrentProcessingDistributionBySimulations() {
    // GIVEN
    final CurrentProcessingDistribution currentProcessingDist = mockCurrentProcDist(
        A_DATE_UTC, 35L);

    entityManager.persistAndFlush(currentProcessingDist);

    // WHEN
    final List<CurrentProcessingDistribution> currentProcessingDistList =
        repository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
            currentProcessingDist.getLogisticCenterId(),
            currentProcessingDist.getWorkflow(), Set.of(currentProcessingDist.getType()),
            List.of(PICKING, PACKING),
            currentProcessingDist.getDate().minusHours(1),
            currentProcessingDist.getDate().plusHours(1));

    // THEN
    assertEquals(1, currentProcessingDistList.size());

    final CurrentProcessingDistribution foundCurrentProc = currentProcessingDistList.get(0);
    whenCurrentDistributionIsOk(currentProcessingDist, foundCurrentProc);
  }

  @Test
  @DisplayName("Deactivate processing distribution")
  void testDeactivateProcessingDistribution() {
    // GIVEN
    final CurrentProcessingDistribution currentProcessingDist = mockCurrentProcDist(A_DATE_UTC, 35L);
    entityManager.persistAndFlush(currentProcessingDist);

    // WHEN
    repository.deactivateProcessingDistribution(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        PACKING,
        singletonList(A_DATE_UTC),
        ACTIVE_WORKERS,
        USER_ID,
        WORKERS
    );

    final Optional<CurrentProcessingDistribution> result = repository.findById(1L);

    // THEN
    assertTrue(result.isPresent());
    assertFalse(result.get().isActive());
    assertEquals(USER_ID, result.get().getUserId());
  }

  @Test
  @DisplayName("Deactivate processing distribution for range of dates")
  void testDeactivateProcessingDistributionForRangeOfDates() {
      // GIVEN
      final List<CurrentProcessingDistribution> currentProcessingDistributions = mockCurrentProcessingDistributions();
      currentProcessingDistributions.forEach(
              currentProcessingDistribution -> entityManager.persistAndFlush(currentProcessingDistribution)
      );

      // WHEN
      repository.deactivateProcessingDistributionForRangeOfDates(
              WAREHOUSE_ID,
              DEACTIVATE_DATE_FROM,
              DEACTIVATE_DATE_TO,
              USER_ID
      );

      final List<CurrentProcessingDistribution> results = repository.findAll();

      // THEN
      assertFalse(results.isEmpty());
      assertEquals(2,
              results.stream()
                      .filter(CurrentProcessingDistribution::isActive)
                      .count()
      );
      assertEquals(1,
              results.stream()
                      .filter(currentProcessingDistribution -> !currentProcessingDistribution.isActive())
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
        Set.of(ProcessPath.GLOBAL.name()),
        Set.of(PICKING.name(), PACKING.name()),
        Set.of(ACTIVE_WORKERS.name()),
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
        Set.of(ProcessPath.GLOBAL.name()),
        Set.of(PICKING.name(), PACKING.name()),
        Set.of(ACTIVE_WORKERS.name()),
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
    assertEquals(10L, picking.get().getQuantity());


    final var packing = result.stream()
        .filter(p -> p.getProcessName() == PACKING)
        .findFirst();

    assertTrue(packing.isPresent());
    assertEquals(10L, packing.get().getQuantity());
  }

  private void whenCurrentDistributionIsOk(final CurrentProcessingDistribution currentProcessing,
                                           final CurrentProcessingDistribution foundCurrentProc) {
    assertEquals(currentProcessing.getId(), foundCurrentProc.getId());
    assertEquals(currentProcessing.getWorkflow(), foundCurrentProc.getWorkflow());
    assertEquals(currentProcessing.getType(), foundCurrentProc.getType());
    assertEquals(currentProcessing.getDate(), foundCurrentProc.getDate());
    assertEquals(currentProcessing.getProcessName(), foundCurrentProc.getProcessName());
    assertEquals(currentProcessing.isActive(), foundCurrentProc.isActive());
    assertEquals(currentProcessing.getQuantity(), foundCurrentProc.getQuantity());
    assertEquals(currentProcessing.getQuantityMetricUnit(),
        foundCurrentProc.getQuantityMetricUnit());
    assertEquals(currentProcessing.getLogisticCenterId(),
        foundCurrentProc.getLogisticCenterId());
  }
}
