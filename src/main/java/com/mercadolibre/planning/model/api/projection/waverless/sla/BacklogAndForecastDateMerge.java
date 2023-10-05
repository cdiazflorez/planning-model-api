package com.mercadolibre.planning.model.api.projection.waverless.sla;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.Merger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BacklogAndForecastDateMerge implements Merger {

  @Override
  public BacklogAndForecastByDate merge(final Backlog... backlogs) {
    if (backlogs.length == 0) {
      return new BacklogAndForecastByDate(Collections.emptyMap());
    } else {
      return mergeBacklogs(backlogs);
    }
  }

  private BacklogAndForecastByDate mergeBacklogs(final Backlog... backlogs) {
    final Map<Instant, BacklogAndForecastByDate.BacklogAndForecast> backlogByDate = Arrays.stream(backlogs)
        .filter(Objects::nonNull)
        .map(BacklogAndForecastByDate.class::cast)
        .map(BacklogAndForecastByDate::getBacklogs)
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            BacklogAndForecastByDate.Mergeable::merge
        ));

    return new BacklogAndForecastByDate(backlogByDate);
  }
}
