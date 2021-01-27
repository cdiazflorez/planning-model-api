package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import com.mercadolibre.planning.model.api.gateway.PlanningDistributionGateway;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.api.util.EntitiesUtil.paginate;
import static java.util.stream.Collectors.toList;

@Component
@AllArgsConstructor
public class PlanningDistributionJpaRepository implements PlanningDistributionGateway {

    private static final int INSERT_SIZE = 1000;

    private final EntityManager entityManager;

    @Trace
    @Override
    public void create(final List<PlanningDistribution> entities, final long forecastId) {
        final List<List<?>> pages = paginate(entities, INSERT_SIZE);

        pages.forEach(page -> {
            final Query query = entityManager.createNativeQuery(
                    getPlanningDistributionQuery(page.size())
            );
            int paramIndex = 1;
            for (final Object object : page) {
                final var entity = (PlanningDistribution) object;

                query.setParameter(paramIndex++, forecastId);
                query.setParameter(paramIndex++, entity.getDateIn());
                query.setParameter(paramIndex++, entity.getDateOut());
                query.setParameter(paramIndex++, entity.getQuantity());
                query.setParameter(paramIndex++, entity.getQuantityMetricUnit().name());
            }
            query.executeUpdate();
        });

        final List<Long> ids = getPlanningDistributionIds(forecastId);
        createMetadata(buildMetadataList(ids, entities));
    }

    private List<Long> getPlanningDistributionIds(final long forecastId) {
        final Query query = entityManager.createNativeQuery(
                "SELECT id FROM planning_distribution "
                        + "WHERE forecast_id = :forecastId "
                        + "ORDER BY id ASC"
        );
        query.setParameter("forecastId", forecastId);

        final List<BigInteger> result = query.getResultList();
        return result.stream().map(BigInteger::longValue).collect(toList());
    }

    private List<PlanningDistributionMetadata> buildMetadataList(
            final List<Long> ids,
            final List<PlanningDistribution> entities) {
        final List<PlanningDistributionMetadata> metadataList = new ArrayList<>();

        IntStream.range(0, ids.size()).forEach(index -> {
            entities.get(index).getMetadatas().forEach(metadata ->
                    metadata.setPlanningDistributionId(ids.get(index)));
            metadataList.addAll(entities.get(index).getMetadatas());
        });
        return metadataList;
    }

    @Trace
    private void createMetadata(final List<PlanningDistributionMetadata> metadataList) {
        final List<List<?>> pages = paginate(metadataList, INSERT_SIZE);

        pages.forEach(page -> {
            final Query query = entityManager.createNativeQuery(
                    getPlanningDistributionMetadataQuery(page.size())
            );
            int paramIndex = 1;
            for (final Object object : page) {
                final var metadata = (PlanningDistributionMetadata) object;
                query.setParameter(paramIndex++, metadata.getPlanningDistributionId());
                query.setParameter(paramIndex++, metadata.getKey());
                query.setParameter(paramIndex++, metadata.getValue());
            }
            query.executeUpdate();
        });
    }

    private String getPlanningDistributionQuery(final int size) {
        final String query = "INSERT INTO planning_distribution "
                + "(forecast_id, date_in, date_out, quantity, quantity_metric_unit) "
                + "VALUES " + "(?,?,?,?,?),".repeat(size);

        return query.substring(0, query.length() - 1);
    }

    protected String getPlanningDistributionMetadataQuery(final int size) {
        final String query = "INSERT INTO planning_distribution_metadata "
                + "(planning_distribution_id, `key`, `value`) "
                + "VALUES " + "(?,?,?),".repeat(size);

        return query.substring(0, query.length() - 1);
    }
}
