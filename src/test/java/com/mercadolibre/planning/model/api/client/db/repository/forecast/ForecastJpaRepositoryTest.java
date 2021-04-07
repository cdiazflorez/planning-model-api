package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
public class ForecastJpaRepositoryTest {

    @Autowired
    EntityManager entityManager;

    @Test
    public void testDeleteOlderThan() {
        // GIVEN
        ForecastJpaRepository repository = new ForecastJpaRepository(entityManager);

        Forecast firstForecast = mockSimpleForecast();
        entityManager.persist(firstForecast);

        Forecast secondForecast = mockSimpleForecast();
        entityManager.persist(secondForecast);

        // WHEN
        repository.deleteOlderThan(FBM_WMS_OUTBOUND, secondForecast.getLastUpdated());

        // THEN
        List<Forecast> result = entityManager.createQuery("SELECT f FROM Forecast f", Forecast.class)
                .getResultList();

        assertEquals(1,result.size());
        assertEquals(secondForecast.getLastUpdated(), result.get(0).getLastUpdated());
    }
}
