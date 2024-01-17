package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import static com.mercadolibre.planning.model.api.util.EntitiesUtil.paginate;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import com.newrelic.api.agent.Trace;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProcessingDistributionJpaRepository implements ProcessingDistributionGateway {
  private static final String COLUMN_NAMES = "forecast_id, date, process_path, process_name, quantity, quantity_metric_unit, type, tags";

  private static final String COLUMN_PLACEHOLDERS = "(?,?,?,?,?,?,?,?),";

  private static final int INSERT_SIZE = 1000;

  private final EntityManager entityManager;

  @Trace
  @Override
  public void create(final List<ProcessingDistribution> entities, final long forecastId) {
    final List<List<ProcessingDistribution>> pages = paginate(entities, INSERT_SIZE);

    pages.forEach(page -> {
      final Query query = entityManager.createNativeQuery(getInsertQuery(page.size()));

      int paramIndex = 1;

      for (final ProcessingDistribution entity : page) {
        query.setParameter(paramIndex++, forecastId);
        query.setParameter(paramIndex++, entity.getDate());
        query.setParameter(paramIndex++, entity.getProcessPath().name());
        query.setParameter(paramIndex++, entity.getProcessName().name());
        query.setParameter(paramIndex++, entity.getQuantity());
        query.setParameter(paramIndex++, entity.getQuantityMetricUnit().name());
        query.setParameter(paramIndex++, entity.getType().name());
        query.setParameter(paramIndex++, entity.getTags());
      }
      query.executeUpdate();
    });
  }

  private String getInsertQuery(final int size) {
    final String query = "INSERT INTO processing_distribution (" + COLUMN_NAMES + ") VALUES " + COLUMN_PLACEHOLDERS.repeat(size);
    return query.substring(0, query.length() - 1);
  }
}
