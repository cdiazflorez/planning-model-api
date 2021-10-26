package com.mercadolibre.planning.model.api.client.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.api.client.rest.config.RestPool;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.gateway.RouteEtsGateway;

import com.mercadolibre.restclient.MeliRestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.OK;

public class RouteEtsClient extends HttpClient implements RouteEtsGateway {

    private static final String URL = "/multisearch/estimated-times?from=%s";

    private final ObjectMapper objectMapper;

    public RouteEtsClient(final MeliRestClient client, final ObjectMapper objectMapper) {
        super(client, RestPool.ROUTE_ETS.name());
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RouteEtsDto> getRoutEts(final String warehouseId) {
        final HttpRequest request = HttpRequest.builder()
                .url(format(URL, warehouseId))
                .GET()
                .acceptedHttpStatuses(Set.of(OK))
                .build();

        final List<Object> apiResponse = send(request, response ->
                response.getData(new TypeReference<>() {})
        );

        return parsearListRouteEts(apiResponse);
    }

    private List<RouteEtsDto> parsearListRouteEts(final List apiResponse) {

        final List<RouteEtsDto> list = new ArrayList<>(apiResponse.size());

        for (final Object obj: apiResponse) {
            list.add(objectMapper.convertValue(obj, RouteEtsDto.class));
        }

        return list;
    }

}
