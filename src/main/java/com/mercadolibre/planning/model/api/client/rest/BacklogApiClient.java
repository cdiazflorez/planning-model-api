package com.mercadolibre.planning.model.api.client.rest;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.api.client.rest.config.RestPool;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.BacklogPhoto;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import com.mercadolibre.restclient.MeliRestClient;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.springframework.http.HttpStatus.OK;

@Component
public class BacklogApiClient extends HttpClient implements BacklogGateway {
    private static final String BACKLOG_URL = "/backlogs/logistic_centers/%s/backlogs/current";
    private static final String OUTBOUND_ORDERS = "outbound-orders";
    private static final String INBOUND = "inbound";

    private static final Map<Workflow, String> WORKFLOW_BY_ALIAS_WORKFLOW = Map.of(
            FBM_WMS_OUTBOUND, OUTBOUND_ORDERS,
            FBM_WMS_INBOUND, INBOUND
    );

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

    private Map<String, String> getQueryParams(final List<Workflow> requestedWorkflows,
                                               final List<String> steps,
                                               final Instant slaFrom,
                                               final Instant slaTo,
                                               final List<String> groupingFields) {

        final List<String> workflows = requestedWorkflows == null
                ? Collections.emptyList()
                : requestedWorkflows
                    .stream()
                    .map(WORKFLOW_BY_ALIAS_WORKFLOW::get)
                    .collect(Collectors.toList());

        final Map<String, String> params = new HashMap<>();
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
