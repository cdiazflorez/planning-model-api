package com.mercadolibre.planning.model.api.domain.entity;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PhotoRequest {

  static final String LOGISTIC_CENTER_ID = "logistic_center_id";
  static final String WORKFLOWS = "workflows";
  static final String STEPS = "steps";
  static final String DATE_FROM = "date_from";
  static final String DATE_TO = "date_to";
  static final String DATE_IN_FROM = "date_in_from";
  static final String DATE_IN_TO = "date_in_to";
  static final String DATE_OUT_FROM = "date_out_from";
  static final String DATE_OUT_TO = "date_out_to";
  static final String GROUP_BY = "group_by";


  protected List<Workflow> workflows;
  protected String logisticCenterId;
  protected List<String> steps;
  protected Instant dateFrom;
  protected Instant dateTo;
  protected Instant dateInFrom;
  protected Instant dateInTo;
  protected Instant dateOutFrom;
  protected Instant dateOutTo;
  protected List<BacklogGrouper> groupBy;

  /**
   * Get query params photos.
   *
   * @return params in a map
   */
  public Map<String, String> getQueryParamsPhoto() {
    final Map<String, String> params = new ConcurrentHashMap<>();

    params.put(LOGISTIC_CENTER_ID, logisticCenterId);
    addAsQueryParam(params, WORKFLOWS, workflows.stream().map(Workflow::getAlias).collect(Collectors.toList()));
    addAsQueryParam(params, STEPS, steps);
    addAsQueryParam(params, DATE_FROM, dateFrom);
    addAsQueryParam(params, DATE_TO, dateTo);
    addAsQueryParam(params, DATE_IN_FROM, dateInFrom);
    addAsQueryParam(params, DATE_IN_TO, dateInTo);
    addAsQueryParam(params, DATE_OUT_FROM, dateOutFrom);
    addAsQueryParam(params, DATE_OUT_TO, dateOutTo == null ? null : dateOutTo.truncatedTo(ChronoUnit.SECONDS));
    addAsQueryParam(params, GROUP_BY, groupBy.stream().map(BacklogGrouper::getName).collect(Collectors.toList()));

    return params;
  }

  protected void addAsQueryParam(final Map<String, String> map, final String key, final List<String> value) {
    if (value != null) {
      map.put(key, String.join(",", value));
    }
  }

  protected void addAsQueryParam(final Map<String, String> map, final String key, final Instant value) {
    if (value != null) {
      map.put(key, ISO_INSTANT.format(value));
    }
  }


}
