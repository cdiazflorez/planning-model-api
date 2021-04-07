package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.gateway.ForecastGateway;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;

import java.time.ZonedDateTime;
import java.util.List;

@Component
@AllArgsConstructor
public class ForecastJpaRepository implements ForecastGateway {

    private final EntityManager entityManager;

    @Trace
    @Override
    public Forecast create(final Forecast forecast, final List<ForecastMetadata> metadataList) {
        entityManager.persist(forecast);

        metadataList.forEach(metadata -> {
            metadata.setForecastId(forecast.getId());
            entityManager.persist(metadata);
        });
        return forecast;
    }

    @Override
    public int deleteOlderThan(final Workflow workflow, final ZonedDateTime date) {
        final String queryStr =
                "SELECT f FROM Forecast f WHERE f.lastUpdated < :date AND f.workflow = :workflow";

        final List<Forecast> oldForecasts = entityManager.createQuery(queryStr, Forecast.class)
                .setParameter("date", date)
                .setParameter("workflow", workflow)
                .getResultList();

        oldForecasts.forEach(entityManager::remove);

        return oldForecasts.size();
    }
}
