package com.mercadolibre.planning.model.api.client.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.RequestBodyHandler;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.api.client.rest.config.RestPool;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.routeets.RouteEtsRequest;
import com.mercadolibre.planning.model.api.gateway.RouteEtsGateway;

import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.exception.ParseException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.OK;

@Component
public class RouteEtsClient extends HttpClient implements RouteEtsGateway {

    private static final String URL = "/multisearch/estimated-times";

    private final ObjectMapper objectMapper;

    public RouteEtsClient(final MeliRestClient client, final ObjectMapper objectMapper) {
        super(client, RestPool.ROUTE_ETS.name());
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RouteEtsDto> postRoutEts(final RouteEtsRequest routeEtsRequest) {
        final HttpRequest request = HttpRequest.builder()
                .url(URL)
                .POST(requestSupplier(routeEtsRequest))
                .acceptedHttpStatuses(Set.of(OK))
                .build();

        final List<Object> apiResponse = send(request, response ->
                response.getData(new TypeReference<>() {})
        );

        return parseListRouteEts(apiResponse);
    }

    private List<RouteEtsDto> parseListRouteEts(final List apiResponse) {

        final List<RouteEtsDto> list = new ArrayList<>(apiResponse.size());

        for (final Object obj: apiResponse) {
            list.add(objectMapper.convertValue(obj, RouteEtsDto.class));
        }

        return list;
    }


    private <T> RequestBodyHandler requestSupplier(final T requestBody) {
        return () -> {
            try {
                return objectMapper.writeValueAsBytes(requestBody);
            } catch (JsonProcessingException e) {
                throw new ParseException(e);
            }
        };
    }

}
