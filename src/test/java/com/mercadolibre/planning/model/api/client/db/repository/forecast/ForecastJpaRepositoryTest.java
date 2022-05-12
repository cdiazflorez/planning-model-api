package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.LIMIT;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastMetadata;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;

import java.util.List;

@DataJpaTest
public class ForecastJpaRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testDeleteOlderThan() {
        // GIVEN
        final ForecastJpaRepository repository = new ForecastJpaRepository(entityManager);

        final Forecast firstForecast = mockSimpleForecast();
        entityManager.persist(firstForecast);

        final ForecastMetadata firstForecastMetadata = mockForecastMetadata(firstForecast);
        entityManager.persist(firstForecastMetadata);

        final Forecast secondForecast = mockSimpleForecast();
        entityManager.persist(secondForecast);

        final ForecastMetadata secondForecastMetadata = mockForecastMetadata(secondForecast);
        entityManager.persist(secondForecastMetadata);

        // WHEN
        repository.deleteOlderThan(FBM_WMS_OUTBOUND, secondForecast.getLastUpdated(), LIMIT);

        // THEN
        final List<Forecast> result =
                entityManager.createQuery("SELECT f FROM Forecast f", Forecast.class)
                        .getResultList();

        assertEquals(1,result.size());
        assertEquals(secondForecast.getLastUpdated(), result.get(0).getLastUpdated());
    }
}
