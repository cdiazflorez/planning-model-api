package com.mercadolibre.planning.model.api.adapter.units;

import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.DATE_IN;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.PATH;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.WORKFLOW;

import com.mercadolibre.planning.model.api.domain.entity.LastPhotoRequest;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.Photo;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Entry point for apply inbound deviations
 *
 * <p>Provides map from backlog to be apply deviations easiest.</p>
 */
@Service
@AllArgsConstructor
public class InboundScheduledBacklogAdapter implements PlannedBacklogService.InboundScheduledBacklogGateway {

  private final BacklogGateway backlogGateway;

  @Override
  public List<PlannedBacklogService.InboundScheduledBacklog> getScheduledBacklog(
      final String warehouseId,
      final List<Workflow> workflows,
      final Instant dateFrom,
      final Instant dateTo,
      final Instant viewDate
  ) {

    final Photo photo = backlogGateway.getLastPhoto(
        new LastPhotoRequest(
            workflows,
            warehouseId,
            List.of("SCHEDULED"),
            null,
            null,
            null,
            null,
            dateFrom,
            dateTo,
            List.of(DATE_IN, DATE_OUT, WORKFLOW, PATH),
            viewDate
        )
    );

    return photo == null
        ? Collections.emptyList()
        : photo.getGroups().stream().map(this::fromGroupToInboundScheduledBacklog).collect(Collectors.toList());
  }

  private PlannedBacklogService.InboundScheduledBacklog fromGroupToInboundScheduledBacklog(Photo.Group group) {
    return new PlannedBacklogService.InboundScheduledBacklog(
        group.getWorkflow().orElseThrow(),
        group.getDateIn().orElseThrow(),
        group.getDateOut().orElseThrow(),
        group.getPath().orElse(null),
        group.getTotal(),
        group.getAccumulatedTotal()
    );
  }
}
