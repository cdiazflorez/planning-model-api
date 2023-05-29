package com.mercadolibre.planning.model.api.projection.backlogmanager;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.Merger;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcessPathMerger implements Merger {

  private final Merger delegateMerger;

  /**
   * Merges several {@link OrderedBacklogByProcessPath} by delegating the merge operation of all the backlogs
   * with the same Process Path to its delegate.
   *
   * @param backlogs two or more backlogs to be merged.
   * @return OrderedBacklogByProcessPath.
   */
  @Override
  public Backlog merge(Backlog... backlogs) {
    if (backlogs.length == 0) {
      return new OrderedBacklogByDate(Collections.emptyMap());
    } else {
      return mergeBacklogs(backlogs);
    }
  }

  public Backlog mergeBacklogs(Backlog... backlogs) {
    final Map<ProcessPath, Backlog> backlogsByProcessPath = Arrays.stream(backlogs)
        .map(OrderedBacklogByProcessPath.class::cast)
        .map(OrderedBacklogByProcessPath::getBacklogs)
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .collect(
            groupingBy(
                Map.Entry::getKey,
                mapping(
                    Map.Entry::getValue,
                    collectingAndThen(Collectors.toList(), this::mergeList)
                )
            )
        );

    return new OrderedBacklogByProcessPath(backlogsByProcessPath);
  }

  private Backlog mergeList(final List<Backlog> backlogs) {
    return delegateMerger.merge(backlogs.toArray(new Backlog[0]));
  }

}
