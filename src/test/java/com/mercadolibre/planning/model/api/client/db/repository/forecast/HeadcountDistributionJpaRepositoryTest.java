package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
public class HeadcountDistributionJpaRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testCreate() {

        // GIVEN
        final Forecast forecast = new Forecast();
        entityManager.persist(forecast);

        final HeadcountDistributionJpaRepository repository =
                new HeadcountDistributionJpaRepository(entityManager);

        final HeadcountDistribution entity = HeadcountDistribution.builder()
                .area("PW")
                .processName(ProcessName.PICKING)
                .quantity(10)
                .quantityMetricUnit(MetricUnit.UNITS)
                .build();

        final HeadcountDistribution[] entities = new HeadcountDistribution[1010];
        Arrays.fill(entities, entity);

        //WHEN
        repository.create(Arrays.asList(entities), forecast.getId());

        //THEN
        final Query query = entityManager.createNativeQuery("select * from headcount_distribution",
                HeadcountDistribution.class);

        final List<HeadcountDistribution> persistedEntities = query.getResultList();
        assertEquals(1010, persistedEntities.size());
        assertEquals(entity.getArea(), persistedEntities.get(0).getArea());
        assertEquals(entity.getProcessName(), persistedEntities.get(0).getProcessName());
        assertEquals(entity.getQuantity(), persistedEntities.get(0).getQuantity());
        assertEquals(entity.getQuantityMetricUnit(),
                persistedEntities.get(0).getQuantityMetricUnit());
    }
}
