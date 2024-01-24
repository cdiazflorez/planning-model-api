package com.mercadolibre.planning.model.api.client.db.repository.util;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class StaffingPlanUtil {

  public static final String DATE_COLUMN = "date";

  public static final String TAGS_COLUMN = "tags ->>'$.%s'";

  public static final String COMMA_SEPARATOR = ", ";

  public static final String EMPTY_STRING = "";

  private static final int COLUMN_QUANTITY = 0;

  private static final int COLUMN_QUANTITY_METRIC_UNIT = 1;

  private static final int COLUMN_TYPE = 2;

  private static final int NEXT_COLUMN = 3;

  private StaffingPlanUtil() {
  }

  public static String buildWhereClauseWithJsonExtract(final String column) {
    return format(" AND tags ->> '$.%s' in (:%s)", column, column);
  }

  public static String buildColumnClause(final String clause,
                                         final List<String> groupers) {
    return groupers.isEmpty()
        ? format(clause, EMPTY_STRING)
        : format(clause,
                 COMMA_SEPARATOR.concat(groupers.stream()
                                            .map(grouper -> DATE_COLUMN.equals(grouper)
                                                ? DATE_COLUMN : format(TAGS_COLUMN, grouper))
                                            .collect(joining(COMMA_SEPARATOR))));
  }

  public static List<StaffingPlan> adaptResultToStaffingPlan(final List resultQuery,
                                                             final List<String> groupers) {

    return ((List<Object[]>) resultQuery).stream()
        .map(row -> {

          final double quantity = (Double) row[COLUMN_QUANTITY];
          final MetricUnit metricUnit = MetricUnit.valueOf(((String) row[COLUMN_QUANTITY_METRIC_UNIT]));
          final ProcessingType type = ProcessingType.valueOf((String) row[COLUMN_TYPE]);

          final ConcurrentHashMap<String, String> groupersMap = new ConcurrentHashMap<>();

          groupers.forEach(grouper -> {
            final Object rowValue = row[NEXT_COLUMN + groupers.indexOf(grouper)];
            if (rowValue != null) {
              if (DATE_COLUMN.equals(grouper)) {
                groupersMap.put(grouper, ((Timestamp) rowValue).toInstant().toString());
              } else {
                groupersMap.put(grouper, (String) rowValue);
              }
            }
          });
          return new StaffingPlan(quantity, metricUnit, type, groupersMap);
        })
        .toList();
  }

}
