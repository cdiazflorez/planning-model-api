package com.mercadolibre.planning.model.api.client.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.api.client.rest.config.RestPool;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralDto;
import com.mercadolibre.planning.model.api.gateway.DeferralGateway;
import com.mercadolibre.restclient.MeliRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
public class FlowMonitorClient extends HttpClient implements DeferralGateway {

    private static final String URL = "/warehouses/%s/workflows/%s";

    private final ObjectMapper objectMapper;

    public FlowMonitorClient(final MeliRestClient client, final ObjectMapper objectMapper) {
        super(client, RestPool.FLOW_MONITOR.name());
        this.objectMapper = objectMapper;
    }

    @Override
    public DeferralDto getDeferralProjection(final String warehouseId, final Workflow workflow) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(URL + "/projections", warehouseId, workflow))
                .GET()
                .acceptedHttpStatuses(Set.of(OK))
                .build();

        final Object apiResponse = send(request, response ->
                response.getData(new TypeReference<>() {})
        );

        return objectMapper.convertValue(apiResponse, DeferralDto.class);
    }
}
