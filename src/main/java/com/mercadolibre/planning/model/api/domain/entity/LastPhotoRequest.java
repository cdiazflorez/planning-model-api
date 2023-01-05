package com.mercadolibre.planning.model.api.domain.entity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class LastPhotoRequest extends PhotoRequest {
  static final String PHOTO_DATE_TO = "photo_date_to";

  Instant photoDateTo;

  public LastPhotoRequest(final List<Workflow> workflows,
                          final String logisticCenterId,
                          final List<String> steps,
                          final Instant dateFrom,
                          final Instant dateTo,
                          final Instant dateInFrom,
                          final Instant dateInTo,
                          final Instant dateOutFrom,
                          final Instant dateOutTo,
                          final List<BacklogGrouper> groupBy, Instant photoDateTo) {
    super(workflows, logisticCenterId, steps, dateFrom, dateTo, dateInFrom, dateInTo, dateOutFrom, dateOutTo, groupBy);
    this.photoDateTo = photoDateTo;
  }

  /**
   * Get query params last photo.
   *
   * @return params in a map
   */
  public Map<String, String> getQueryParamsPhoto() {
    final Map<String, String> params = new ConcurrentHashMap<>();

    params.put(LOGISTIC_CENTER_ID, logisticCenterId);
    addAsQueryParam(params, PHOTO_DATE_TO, photoDateTo);
    addAsQueryParam(params, WORKFLOWS, workflows.stream().map(Workflow::getAlias).collect(Collectors.toList()));
    addAsQueryParam(params, STEPS, steps);
    addAsQueryParam(params, DATE_OUT_FROM, dateOutFrom);
    addAsQueryParam(params, DATE_OUT_TO, dateOutTo);
    addAsQueryParam(params, DATE_IN_FROM, dateInFrom);
    addAsQueryParam(params, DATE_IN_TO, dateInTo);
    addAsQueryParam(params, GROUP_BY, groupBy.stream().map(BacklogGrouper::getName).collect(Collectors.toList()));

    return params;
  }
}
