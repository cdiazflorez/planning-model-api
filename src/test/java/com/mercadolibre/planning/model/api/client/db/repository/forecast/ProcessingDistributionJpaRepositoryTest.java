package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static java.util.Arrays.fill;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class ProcessingDistributionJpaRepositoryTest {

  private static final ZonedDateTime DATE = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);

  @Autowired
  private EntityManager entityManager;

  @Test
  void testCreate() {
    // GIVEN
    final Forecast forecast = new Forecast();
    entityManager.persist(forecast);

    final ProcessingDistributionJpaRepository repository = new ProcessingDistributionJpaRepository(entityManager);

    final ProcessingDistribution entity = ProcessingDistribution.builder()
        .date(DATE)
        .processPath(ProcessPath.GLOBAL)
        .processName(ProcessName.PICKING)
        .quantity(10)
        .quantityMetricUnit(MetricUnit.UNITS)
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
}
