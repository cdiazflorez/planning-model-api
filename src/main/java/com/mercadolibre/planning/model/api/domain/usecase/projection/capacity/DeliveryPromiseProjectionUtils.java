package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public final class DeliveryPromiseProjectionUtils {
  private DeliveryPromiseProjectionUtils() {

  }

  static List<ZonedDateTime> getCptDefaultFromBacklog(final List<Backlog> backlogs) {
    return backlogs == null
        ? emptyList()
        : backlogs.stream().map(Backlog::getDate).distinct().collect(toList());
  }

  static List<ZonedDateTime> getSlasToBeProjectedFromBacklogAndKnowSlas(final List<Backlog> backlogs,
                                                                        final List<GetSlaByWarehouseOutput> allCptByWarehouse) {
    return Stream.of(
            backlogs.stream()
                .map(Backlog::getDate),
            allCptByWarehouse.stream()
                .map(GetSlaByWarehouseOutput::getDate)
        )
        .flatMap(Function.identity())
        .map(date -> date.withZoneSameInstant(ZoneOffset.UTC))
        .distinct()
        .collect(toList());
  }
}
