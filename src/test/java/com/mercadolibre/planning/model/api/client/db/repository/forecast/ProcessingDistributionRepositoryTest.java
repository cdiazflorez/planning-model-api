package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProcessingDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.adapter.headcount.ProcessPathShareAdapter.ShareView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
class ProcessingDistributionRepositoryTest {

  private static final Instant DATE_FROM = Instant.parse("2022-09-08T10:00:00Z");

  private static final Instant MIDDLE_DATE = Instant.parse("2022-09-08T11:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2022-09-08T12:00:00Z");

  private static final Map<ProcessPath, Map<Instant, Double>> EXPECTED_SHARE_RESULTS = Map.of(
      GLOBAL, Map.of(
          DATE_FROM, 1.0,
          MIDDLE_DATE, 1.0,
          DATE_TO, 1.0
      ),
      TOT_MONO, Map.of(
          DATE_FROM, 0.700,
          MIDDLE_DATE, 0.600,
          DATE_TO, 0.066
      ),
      TOT_MULTI_BATCH, Map.of(
          DATE_FROM, 0.3,
          MIDDLE_DATE, 0.4
      ),
      NON_TOT_MONO, Map.of(
          DATE_TO, 0.933
      )
  );

  @Autowired
  private ProcessingDistributionRepository repository;

  @Autowired
  private TestEntityManager entityManager;

  private static void assertResults(final List<ShareView> views) {
    for (final ShareView view : views) {
      final var expected = EXPECTED_SHARE_RESULTS.get(view.getProcessPath()).get(view.getDate());

      assertEquals(PICKING, view.getProcessName());
      assertEquals(expected, view.getShare(), 0.001);
    }
  }

  @Test
  @DisplayName("Looking for a processing distribution that exists, returns it")
  void testFindProcessingDistributionById() {
    // GIVEN
    final Forecast forecast = mockSimpleForecast();
    entityManager.persistAndFlush(forecast);
    final ProcessingDistribution processingDistribution = mockProcessingDist(forecast);
    entityManager.persistAndFlush(processingDistribution);

    // WHEN
    final Optional<ProcessingDistribution> optProcessingDistribution =
        repository.findById(processingDistribution.getId());

    // THEN
    assertTrue(optProcessingDistribution.isPresent());

    final ProcessingDistribution foundProcessingDistribution =
        optProcessingDistribution.get();

    assertEquals(processingDistribution.getId(), foundProcessingDistribution.getId());
    assertEquals(processingDistribution.getDate(), foundProcessingDistribution.getDate());
    assertEquals(processingDistribution.getQuantity(), foundProcessingDistribution.getQuantity());
    assertEquals(processingDistribution.getQuantityMetricUnit().name(), foundProcessingDistribution.getQuantityMetricUnit().name());
    assertEquals(processingDistribution.getProcessName().name(), foundProcessingDistribution.getProcessName().name());
    assertEquals(processingDistribution.getType().name(), foundProcessingDistribution.getType().name());

    final Forecast foundForecast = foundProcessingDistribution.getForecast();
    assertEquals(forecast.getId(), foundForecast.getId());
    assertEquals(forecast.getWorkflow().name(), foundForecast.getWorkflow().name());
  }

  @Test
  @DisplayName("Looking for a processing distribution that doesn't exist, returns nothing")
  void testProcessingDistributionDoesntExist() {
    // WHEN
    final Optional<ProcessingDistribution> optProcessingDist = repository.findById(1L);

    // THEN
    assertFalse(optProcessingDist.isPresent());
  }

  @Test
  @Sql("/sql/forecast/load_forecast_and_metadata.sql")
  void testGetHeadcountProcessPathShare() {
    // GIVEN

    // WHEN
    final var result = repository.getProcessPathHeadcountShare(
        List.of("PICKING"),
        DATE_FROM,
        DATE_TO,
        List.of(1L, 2L)
    );

    // THEN
    assertNotNull(result);

    assertEquals(9, result.size());
    assertResults(result);
  }
}
