package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.gateway.HeadcountProductivityGateway;
import com.mercadolibre.planning.model.api.util.EntitiesUtil;
import com.newrelic.api.agent.Trace;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class HeadcountProductivityJpaRepository implements HeadcountProductivityGateway {

  private static final String COLUMN_NAMES =
      "forecast_id, date, process_path, process_name, productivity, productivity_metric_unit, ability_level";

  private static final String COLUMN_PLACEHOLDERS = "(?,?,?,?,?,?,?),";

  private static final int INSERT_SIZE = 1000;

  private final EntityManager entityManager;

  @Trace
  @Override
  public void create(final List<HeadcountProductivity> entities, final long forecastId) {
    final List<List<HeadcountProductivity>> pages = EntitiesUtil.paginate(entities, INSERT_SIZE);

    pages.forEach(page -> {
      final Query query = entityManager.createNativeQuery(getInsertQuery(page.size()));

      int paramIndex = 1;
      for (final HeadcountProductivity entity : page) {
        query.setParameter(paramIndex++, forecastId);
        query.setParameter(paramIndex++, entity.getDate());
        query.setParameter(paramIndex++, entity.getProcessPath().name());
        query.setParameter(paramIndex++, entity.getProcessName().name());
        query.setParameter(paramIndex++, entity.getProductivity());
        query.setParameter(paramIndex++, entity.getProductivityMetricUnit().name());
        query.setParameter(paramIndex++, entity.getAbilityLevel());
      }
      query.executeUpdate();
    });
  }

  private String getInsertQuery(final int size) {
    final String query = "INSERT INTO headcount_productivity ( " + COLUMN_NAMES + ") VALUES " + COLUMN_PLACEHOLDERS.repeat(size);
    return query.substring(0, query.length() - 1);
  }
}
