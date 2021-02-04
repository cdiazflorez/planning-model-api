package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.fill;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
public class ProcessingDistributionJpaRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testCreate() {

        // GIVEN
        final Forecast forecast = new Forecast();
        entityManager.persist(forecast);

        final ProcessingDistributionJpaRepository repository =
                new ProcessingDistributionJpaRepository(entityManager);

        final ProcessingDistribution entity = ProcessingDistribution.builder()
                .date(ZonedDateTime.now())
                .processName(ProcessName.PICKING)
                .quantity(10)
                .quantityMetricUnit(MetricUnit.UNITS)
                .type(ProcessingType.ACTIVE_WORKERS)
                .build();

        final ProcessingDistribution[] entities = new ProcessingDistribution[950];
        fill(entities, entity);

        //WHEN
        repository.create(Arrays.asList(entities), forecast.getId());

        //THEN
        final Query query = entityManager.createNativeQuery(
                "select * from processing_distribution",
                ProcessingDistribution.class);

        final List<ProcessingDistribution> persistedEntities = query.getResultList();
        assertEquals(950, persistedEntities.size());
        assertEquals(entity.getDate(), persistedEntities.get(0).getDate());
        assertEquals(entity.getProcessName(), persistedEntities.get(0).getProcessName());
        assertEquals(entity.getQuantity(), persistedEntities.get(0).getQuantity());
        assertEquals(entity.getQuantityMetricUnit(),
                persistedEntities.get(0).getQuantityMetricUnit());
    }
}
