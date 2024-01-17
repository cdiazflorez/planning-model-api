package com.mercadolibre.planning.model.api.client.db.repository.current;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.PRODUCTIVITY;
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

import com.mercadolibre.planning.model.api.adapter.staffing.GetCreatedMaxDateByType;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

  @Autowired
  private CurrentProcessingDistributionRepository repository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("Looking for a current processing distribution that exists, returns it")
  void testFindCurrentProcessingDistributionById() {
    // GIVEN
    final CurrentProcessingDistribution currentProcessingDist = entityManager.persistAndFlush(mockCurrentProcDist(A_DATE_UTC, 35L));

    // WHEN
    final Optional<CurrentProcessingDistribution> optCurrentProcessingDist =
        repository.findById(currentProcessingDist.getId());

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
    final CurrentProcessingDistribution currentProcessingDistribution = entityManager.persistAndFlush(
        mockCurrentProcDist(A_DATE_UTC, 35L)
    );

    // WHEN
    repository.deactivateProcessingDistribution(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        PACKING,
        singletonList(A_DATE_UTC),
        EFFECTIVE_WORKERS,
        USER_ID,
        WORKERS
    );

    final Optional<CurrentProcessingDistribution> result = repository.findById(currentProcessingDistribution.getId());

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
        FBM_WMS_OUTBOUND,
        DEACTIVATE_DATE_FROM,
        DEACTIVATE_DATE_TO,
        USER_ID
    );

    // THEN
    final List<CurrentProcessingDistribution> results = repository.findAll();

    assertFalse(results.isEmpty());
    assertEquals(3,
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


  @Test
  @Sql("/sql/forecast/load_current_processing_distribution.sql")
  void testLastCreatedDateOfCurrentProcessingDistribution() {
    final ZonedDateTime date = ZonedDateTime.of(2023, 9, 28, 0, 0, 0, 0, ZoneOffset.UTC);

    // GIVE
    final List<CurrentProcessingMaxDateCreatedByType> maxDateEditionByType = List.of(
        new GetCreatedMaxDateByType(EFFECTIVE_WORKERS, date.plusHours(22)),
        new GetCreatedMaxDateByType(EFFECTIVE_WORKERS_NS, date.plusHours(21)),
        new GetCreatedMaxDateByType(PRODUCTIVITY, date.plusHours(23))
    );


    final List<CurrentProcessingMaxDateCreatedByType> dateCreated = repository.findDateCreatedByWarehouseIdAndWorkflowAndTypeAndIsActive(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        Set.of(EFFECTIVE_WORKERS, EFFECTIVE_WORKERS_NS, PRODUCTIVITY),
        date.minusDays(1)
    );

    assertEquals(
        maxDateEditionByType.get(0).getDateCreated().toInstant(),
        dateCreated.stream()
            .filter(current -> current.getType() == maxDateEditionByType.get(0).getType())
            .findFirst().get()
            .getDateCreated().toInstant()
    );

    assertEquals(
        maxDateEditionByType.get(1).getDateCreated().toInstant(),
        dateCreated.stream()
            .filter(current -> current.getType() == maxDateEditionByType.get(1).getType())
            .findFirst().get()
            .getDateCreated().toInstant()
    );

    assertEquals(
        maxDateEditionByType.get(2).getDateCreated().toInstant(),
        dateCreated.stream()
            .filter(current -> current.getType() == maxDateEditionByType.get(2).getType())
            .findFirst().get()
            .getDateCreated().toInstant()
    );
  }

  @Test
  @Sql("/sql/forecast/load_current_processing_distribution.sql")
  void testWithoutLastCreatedDateOfCurrentProcessingDistribution() {
    // GIVE
    final ZonedDateTime date = ZonedDateTime.of(2023, 9, 28, 0, 0, 0, 0, ZoneOffset.UTC);


    final List<CurrentProcessingMaxDateCreatedByType> dateCreated = repository.findDateCreatedByWarehouseIdAndWorkflowAndTypeAndIsActive(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        Set.of(EFFECTIVE_WORKERS, EFFECTIVE_WORKERS_NS, PRODUCTIVITY),
        date.plusDays(1)
    );

    assertTrue(dateCreated.isEmpty());

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
