package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.List;

import static com.mercadolibre.planning.model.api.util.EntitiesUtil.paginate;

@Component
@AllArgsConstructor
public class ProcessingDistributionJpaRepository implements ProcessingDistributionGateway {

    private static final int INSERT_SIZE = 1000;

    private final EntityManager entityManager;

    @Trace
    @Override
    public void create(final List<ProcessingDistribution> entities, final long forecastId) {
        final List<List<?>> pages = paginate(entities, INSERT_SIZE);

        pages.forEach(page -> {
            final Query query = entityManager.createNativeQuery(getInsertQuery(page.size()));

            int paramIndex = 1;
            for (final Object object : page) {
                final ProcessingDistribution entity = (ProcessingDistribution) object;

                query.setParameter(paramIndex++, forecastId);
                query.setParameter(paramIndex++, entity.getDate());
                query.setParameter(paramIndex++, entity.getProcessName().name());
                query.setParameter(paramIndex++, entity.getQuantity());
                query.setParameter(paramIndex++, entity.getQuantityMetricUnit().name());
                query.setParameter(paramIndex++, entity.getType().name());
            }
            query.executeUpdate();
        });
    }

    private String getInsertQuery(final int size) {
        final String query = "INSERT INTO processing_distribution "
                + "(forecast_id, date, process_name, quantity, quantity_metric_unit, type) "
                + "VALUES " + "(?,?,?,?,?,?),".repeat(size);

        return query.substring(0, query.length() - 1);
    }
}
