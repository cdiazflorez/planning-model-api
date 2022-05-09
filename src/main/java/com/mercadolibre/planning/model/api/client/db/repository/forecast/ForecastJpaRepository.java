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
    public int deleteOlderThan(final Workflow workflow, final ZonedDateTime date, final String warehouseId) {
        final String queryStr = String.join(" ",
                "DELETE FROM forecast f",
                "WHERE f.last_updated < ?1",
                "AND f.workflow = ?2",
                "AND f.id in (SELECT fm.forecast_id FROM forecast_metadata fm",
                "WHERE fm.`key` = 'warehouse_id'",
                "AND fm.value = ?3)");

        return entityManager.createNativeQuery(queryStr)
                .setParameter(1, date)
                .setParameter(2, workflow.name())
                .setParameter(3, warehouseId)
                .executeUpdate();
    }
}
