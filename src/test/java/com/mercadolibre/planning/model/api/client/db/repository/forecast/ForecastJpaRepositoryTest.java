package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.LIMIT;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import javax.persistence.EntityManager;

import java.util.List;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ForecastJpaRepositoryTest {

    private static final String LOAD_FORECAST = "/sql/forecast/load_forecast.sql";

    private static final ZonedDateTime DATE_TIME = ZonedDateTime.of(2024, 1, 9, 12, 0, 0, 0, ZoneId.of("UTC"));

    @Autowired
    private EntityManager entityManager;

    @Test
    @Sql(LOAD_FORECAST)
    public void testDeleteOlderThan() {
        // GIVEN
        final ForecastJpaRepository repository = new ForecastJpaRepository(entityManager);

        // WHEN
        repository.deleteOlderThan(FBM_WMS_OUTBOUND, DATE_TIME, LIMIT);

        // THEN
        final List<Forecast> result =
                entityManager.createQuery("SELECT f FROM Forecast f", Forecast.class)
                        .getResultList();

        assertEquals(1,result.size());
        assertEquals(DATE_TIME.withZoneSameInstant(UTC), result.get(0).getLastUpdated().withZoneSameInstant(UTC));
    }
}
