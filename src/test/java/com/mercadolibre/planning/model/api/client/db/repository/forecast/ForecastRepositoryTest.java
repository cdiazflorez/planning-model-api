package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastMetadata;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountProd;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProcessingDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
public class ForecastRepositoryTest {

  private static final Instant VIEW_DATE = Instant.parse("2022-09-08T12:31:00Z");

  private static final Set<String> WEEKS = Set.of("34-2022", "35-2022");

  private static final String WAREHOUSE_ID = "ARTW01";

  @Autowired
  private ForecastRepository repository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("Looking for a forecast that exists, returns it")
  public void testFindForecastById() {
    // GIVEN
    final Forecast forecast = persistForecast();

    // WHEN
    final Optional<Forecast> optForecast = repository.findById(forecast.getId());

    // THEN
    assertTrue(optForecast.isPresent());

    final Forecast foundForecast = optForecast.get();
    assertEquals(forecast.getId(), foundForecast.getId());
    assertEquals(forecast.getWorkflow(), foundForecast.getWorkflow());
    assertEquals(forecast.getDateCreated(), foundForecast.getDateCreated());
    assertEquals(forecast.getLastUpdated(), foundForecast.getLastUpdated());
    assertEquals(forecast.getMetadatas(), foundForecast.getMetadatas());

    assertEquals(forecast.getPlanningDistributions(),
        foundForecast.getPlanningDistributions());

    assertEquals(forecast.getHeadcountDistributions(),
        foundForecast.getHeadcountDistributions());

    assertEquals(forecast.getProcessingDistributions(),
        foundForecast.getProcessingDistributions());

    assertEquals(forecast.getHeadcountProductivities(),
        foundForecast.getHeadcountProductivities());
  }

  @Test
  @DisplayName("Looking for a forecast that doesn't exist, returns nothing")
  public void testForecastDoesntExist() {
    // WHEN
    final Optional<Forecast> optForecast = repository.findById(1L);

    // THEN
    assertFalse(optForecast.isPresent());
  }

  private Forecast persistForecast() {
    final Forecast forecast = mockSimpleForecast();

    entityManager.persistAndFlush(forecast);

    final ForecastMetadata forecastMetadata = mockForecastMetadata(forecast);
    entityManager.persistAndFlush(forecastMetadata);

    final HeadcountDistribution headcountDistribution = mockHeadcountDist(forecast);
    entityManager.persistAndFlush(headcountDistribution);

    final HeadcountProductivity headcountProductivity = mockHeadcountProd(forecast);
    entityManager.persistAndFlush(headcountProductivity);

    final PlanningDistribution planningDistribution = mockPlanningDist(forecast);
    entityManager.persistAndFlush(planningDistribution);

    final ProcessingDistribution processingDistribution = mockProcessingDist(forecast);
    entityManager.persistAndFlush(processingDistribution);

    forecast.setHeadcountDistributions(Set.of(headcountDistribution));
    forecast.setHeadcountProductivities(Set.of(headcountProductivity));
    forecast.setPlanningDistributions(Set.of(planningDistribution));
    forecast.setProcessingDistributions(Set.of(processingDistribution));
    forecast.setMetadatas(Set.of(forecastMetadata));

    return forecast;
  }

  @Test
  @Sql("/sql/forecast/load_forecast_and_metadata.sql")
  void testSearchForecastWithViewDateShouldReturnForecastsCreatedBeforeViewDate() {
    // GIVEN

    // WHEN
    final var idViews = repository.findForecastIdsByWarehouseIdAAndWorkflowAndWeeksAtViewDate(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND.name(),
        WEEKS,
        VIEW_DATE
    );

    // THEN
    assertEquals(2, idViews.size());

    final var ids = idViews.stream().map(ForecastIdView::getId).collect(Collectors.toSet());
    assertTrue(ids.contains(2L));
    assertTrue(ids.contains(5L));
  }
}
