package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.gateway.HeadcountDistributionGateway;
import com.mercadolibre.planning.model.api.util.EntitiesUtil;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.List;

@Component
@AllArgsConstructor
public class HeadcountDistributionJpaRepository implements HeadcountDistributionGateway {

    private static final int INSERT_SIZE = 1000;

    private final EntityManager entityManager;

    @Trace
    @Override
    public void create(final List<HeadcountDistribution> entities, final long forecastId) {
        final List<List<HeadcountDistribution>> pages = EntitiesUtil.paginate(entities, INSERT_SIZE);

        pages.forEach(page -> {
            final Query query = entityManager.createNativeQuery(getInsertQuery(page.size()));

            int paramIndex = 1;
            for (final HeadcountDistribution entity : page) {
                query.setParameter(paramIndex++, forecastId);
                query.setParameter(paramIndex++, entity.getArea());
                query.setParameter(paramIndex++, entity.getProcessName().name());
                query.setParameter(paramIndex++, entity.getQuantity());
                query.setParameter(paramIndex++, entity.getQuantityMetricUnit().name());
            }
            query.executeUpdate();
        });
    }

    private String getInsertQuery(final int size) {
        final String query = "INSERT INTO headcount_distribution "
                + "(forecast_id, area, process_name, quantity, quantity_metric_unit) "
                + "VALUES " + "(?,?,?,?,?),".repeat(size);

        return query.substring(0, query.length() - 1);
    }
}
