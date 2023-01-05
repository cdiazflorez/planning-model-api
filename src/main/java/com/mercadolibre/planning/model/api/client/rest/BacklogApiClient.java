package com.mercadolibre.planning.model.api.client.rest;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.api.client.rest.config.RestPool;
import com.mercadolibre.planning.model.api.domain.entity.LastPhotoRequest;
import com.mercadolibre.planning.model.api.domain.entity.PhotoRequest;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.BacklogPhoto;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.Photo;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import com.mercadolibre.restclient.MeliRestClient;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BacklogApiClient extends HttpClient implements BacklogGateway {
  private static final String BACKLOG_URL = "/backlogs/logistic_centers/%s/backlogs/current";
  private static final String PHOTO_URL = "/backlogs/logistic_centers/%s/photos";

  public BacklogApiClient(final MeliRestClient client) {
    super(client, RestPool.BACKLOG_API.name());
  }

  @Override
  public List<BacklogPhoto> getCurrentBacklog(final String warehouseId,
                                              final List<Workflow> workflows,
                                              final List<String> steps,
                                              final Instant slaFrom,
                                              final Instant slaTo,
                                              final List<String> groupingFields) {
    final HttpRequest httpRequest = HttpRequest.builder()
        .url(format(BACKLOG_URL, warehouseId))
        .GET()
        .queryParams(getQueryParams(workflows, steps, slaFrom, slaTo, groupingFields))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(httpRequest, response ->
        response.getData(new TypeReference<>() {
        })
    );
  }

  /**
   * Gets the backlog photos in the interval specified by the {@link PhotoRequest}.
   * The cells of the returned photos are filtered and grouped according to the {@link PhotoRequest} parameter
   *
   * @param request request of client photo.
   * @return list of photos obtain for client.
   */
  @Override
  public List<Photo> getPhotos(final PhotoRequest request) {
    final HttpRequest httpRequest = HttpRequest.builder()
        .url(format(PHOTO_URL, request.getLogisticCenterId()))
        .GET()
        .queryParams(request.getQueryParamsPhoto())
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(httpRequest, response ->
        response.getData(new TypeReference<>() {
        })
    );

  }

  /**
   * Gets the backlog of last photo.
   * The cells of the returned last photos is filtered and grouped according to the {@link PhotoRequest} parameter
   *
   * @param request request of client photo.
   * @return photo obtain for client.
   */
  public Photo getLastPhoto(final LastPhotoRequest request) {
    final HttpRequest httpRequest = HttpRequest.builder()
        .url(format(PHOTO_URL + "/last", request.getLogisticCenterId()))
        .GET()
        .queryParams(request.getQueryParamsPhoto())
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(httpRequest, response ->
        response.getData(new TypeReference<>() {
        })
    );
  }

  private Map<String, String> getQueryParams(final List<Workflow> requestedWorkflows,
                                             final List<String> steps,
                                             final Instant slaFrom,
                                             final Instant slaTo,
                                             final List<String> groupingFields) {

    final List<String> workflows = requestedWorkflows == null
        ? Collections.emptyList()
        : requestedWorkflows
        .stream()
        .map(Workflow::getAlias)
        .collect(Collectors.toList());

    final Map<String, String> params = new ConcurrentHashMap<>();
    addAsQueryParam(params, "workflows", workflows);
    addAsQueryParam(params, "steps", steps);
    addAsQueryParam(params, "sla_from", slaFrom);
    addAsQueryParam(params, "sla_to", slaTo);
    addAsQueryParam(params, "group_by", groupingFields);

    return params;
  }

  private void addAsQueryParam(final Map<String, String> map, final String key, final List<String> value) {
    if (value != null) {
      map.put(key, String.join(",", value));
    }
  }

  private void addAsQueryParam(final Map<String, String> map, final String key, final Instant value) {
    if (value != null) {
      map.put(key, ISO_INSTANT.format(value));
    }
  }
}
