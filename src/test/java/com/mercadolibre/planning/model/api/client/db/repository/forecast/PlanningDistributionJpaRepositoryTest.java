package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata.builder;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
public class PlanningDistributionJpaRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testCreate() {

        // GIVEN
        final Forecast forecast = new Forecast();
        entityManager.persist(forecast);

        final PlanningDistributionJpaRepository repository =
                new PlanningDistributionJpaRepositoryMock(entityManager);

        final PlanningDistribution entity = PlanningDistribution.builder()
                .dateIn(ZonedDateTime.now())
                .dateOut(ZonedDateTime.now().plusHours(1))
                .quantity(10L)
                .quantityMetricUnit(MetricUnit.UNITS)
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
        assertEquals(entity.getQuantity(), persistedEntities.get(0).getQuantity());
        assertEquals(entity.getQuantityMetricUnit(),
                persistedEntities.get(0).getQuantityMetricUnit());

        assertEquals(3, persistedMetadata.size());
        IntStream.range(0, 3).forEach(index -> {
            assertEquals(
                    entity.getMetadatas().get(index).getKey(),
                    persistedMetadata.get(index).getKey());
            assertEquals(
                    entity.getMetadatas().get(index).getValue(),
                    persistedMetadata.get(index).getValue());
        });
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
