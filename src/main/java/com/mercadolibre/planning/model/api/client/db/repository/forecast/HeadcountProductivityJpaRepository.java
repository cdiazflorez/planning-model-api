package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.gateway.HeadcountProductivityGateway;
import com.mercadolibre.planning.model.api.util.EntitiesUtil;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.List;

@Component
@AllArgsConstructor
public class HeadcountProductivityJpaRepository implements HeadcountProductivityGateway {

    private static final int INSERT_SIZE = 1000;

    private final EntityManager entityManager;

    @Trace
    @Override
    public void create(final List<HeadcountProductivity> entities, final long forecastId) {
        final List<List<?>> pages = EntitiesUtil.paginate(entities, INSERT_SIZE);

        pages.forEach(page -> {
            final Query query = entityManager.createNativeQuery(getInsertQuery(page.size()));

            int paramIndex = 1;
            for (final Object object : page) {
                final HeadcountProductivity entity = (HeadcountProductivity) object;

                query.setParameter(paramIndex++, forecastId);
                query.setParameter(paramIndex++, entity.getDate());
                query.setParameter(paramIndex++, entity.getProcessName().name());
                query.setParameter(paramIndex++, entity.getProductivity());
                query.setParameter(paramIndex++, entity.getProductivityMetricUnit().name());
                query.setParameter(paramIndex++, entity.getAbilityLevel());
            }
            query.executeUpdate();
        });
    }

    private String getInsertQuery(final int size) {
        final String query = "INSERT INTO headcount_productivity "
                + "(forecast_id, date, process_name, productivity,"
                + " productivity_metric_unit, ability_level) "
                + "VALUES " + "(?,?,?,?,?,?),".repeat(size);

        return query.substring(0, query.length() - 1);
    }
}
