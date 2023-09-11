package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
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
class HeadcountProductivityJpaRepositoryTest {

  private static final ZonedDateTime DATE = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES);

  @Autowired
  private EntityManager entityManager;

  @Test
  void testCreate() {
    // GIVEN
    final Forecast forecast = new Forecast();
    entityManager.persist(forecast);

    final HeadcountProductivityJpaRepository repository = new HeadcountProductivityJpaRepository(entityManager);

    final HeadcountProductivity entity = HeadcountProductivity.builder()
        .date(DATE)
        .processPath(ProcessPath.GLOBAL)
        .processName(ProcessName.PICKING)
        .productivity(10)
        .productivityMetricUnit(MetricUnit.UNITS)
        .abilityLevel(1)
        .build();

    final HeadcountProductivity[] entities = new HeadcountProductivity[1000];
    Arrays.fill(entities, entity);

    //WHEN
    repository.create(Arrays.asList(entities), forecast.getId());

    //THEN
    final Query query = entityManager.createNativeQuery("select * from headcount_productivity", HeadcountProductivity.class);

    final List<HeadcountProductivity> persistedEntities = query.getResultList();
    assertEquals(1000, persistedEntities.size());

    assertEquals(entity.getDate(), persistedEntities.get(0).getDate());
    assertEquals(entity.getProcessName(), persistedEntities.get(0).getProcessName());
    assertEquals(entity.getProcessPath(), persistedEntities.get(0).getProcessPath());
    assertEquals(entity.getProductivity(), persistedEntities.get(0).getProductivity());
    assertEquals(entity.getProductivityMetricUnit(), persistedEntities.get(0).getProductivityMetricUnit());
  }
}
