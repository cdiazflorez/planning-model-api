package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata.builder;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class PlanningDistributionJpaRepositoryTest {

  private static final ZonedDateTime DATE = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);

  @Autowired
  private EntityManager entityManager;

  @Test
  public void testCreate() {

    // GIVEN
    final Forecast forecast = mockSimpleForecast();
    entityManager.persist(forecast);

    final PlanningDistributionJpaRepository repository =
        new PlanningDistributionJpaRepository(entityManager);

    final PlanningDistribution entity = PlanningDistribution.builder()
        .dateIn(DATE)
        .dateOut(DATE.plusHours(1))
        .quantity(10.11)
        .quantityMetricUnit(MetricUnit.UNITS)
        .processPath(ProcessPath.TOT_MONO)
        .metadatas(List.of(
            builder().key("carrier_id").value("c-1").build(),
            builder().key("service_id").value("s-1").build(),
            builder().key("canalization").value("ca").build()))
        .build();

    //WHEN
    repository.create(singletonList(entity), forecast.getId());

    //THEN
    final List<PlanningDistribution> persistedEntities = getPlanningDistributions();
    final List<PlanningDistributionMetadata> persistedMetadata =
        getPlanningDistributionsMetadata();

    assertEquals(1, persistedEntities.size());
    assertEquals(entity.getDateIn(), persistedEntities.get(0).getDateIn());
    assertEquals(entity.getDateOut(), persistedEntities.get(0).getDateOut());
    assertEquals(entity.getProcessPath(), persistedEntities.get(0).getProcessPath());
    assertEquals(entity.getQuantity(), persistedEntities.get(0).getQuantity());
    assertEquals(entity.getQuantityMetricUnit(),
                 persistedEntities.get(0).getQuantityMetricUnit());

    assertEquals(3, persistedMetadata.size());
    assertTrue(
        entity.getMetadatas().containsAll(persistedMetadata)
        && persistedMetadata.containsAll(entity.getMetadatas())
    );
  }

  private List<PlanningDistribution> getPlanningDistributions() {
    final Query query = entityManager.createNativeQuery(
        "select * from planning_distribution",
        PlanningDistribution.class);

    return query.getResultList();
  }

  private List<PlanningDistributionMetadata> getPlanningDistributionsMetadata() {
    final Query query = entityManager.createNativeQuery(
        "select * from planning_distribution_metadata",
        PlanningDistributionMetadata.class);

    return query.getResultList();
  }
}
