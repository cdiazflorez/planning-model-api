package com.mercadolibre.planning.model.api.client.rest;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.api.client.rest.config.RestPool;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.RouteCoveragePage;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.RouteCoverageResult;
import com.mercadolibre.planning.model.api.gateway.RouteCoverageClientGateway;
import com.mercadolibre.restclient.MeliRestClient;
import com.newrelic.api.agent.Trace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
public class RouteCoverageClient extends HttpClient implements RouteCoverageClientGateway {

    private static final String URL = "/shipping/routes/rules";
    private static final Map<String, String> siteByWarehousePrefix = Map.of(
            "AR", "MLA",
            "MX", "MLM",
            "CL", "MLC",
            "CO", "MCO",
            "BR", "MLB"
    );

    public RouteCoverageClient(final MeliRestClient client) {
        super(client, RestPool.ROUTE_COVERAGE.name());
    }

    @Trace
    @Override
    public List<RouteCoverageResult> get(String warehouse) {

        final Map<String, String> queryParameters = new HashMap<>();

        queryParameters.put("status", "active");
        queryParameters.put("site", siteByWarehousePrefix.get(warehouse.substring(0, 2)));
        queryParameters.put("from", warehouse);

        final RouteCoveragePage routeCoverage = getActives(queryParameters);

        queryParameters.put("scroll_id", routeCoverage.getScrollId());

        boolean resultIsEmpty = routeCoverage.getResults().isEmpty();

        final List<RouteCoverageResult> routeCoverageResultList =
                new ArrayList<>(routeCoverage.getResults());

        while (!resultIsEmpty) {

            final List<RouteCoverageResult> routeCoverageResults =
                    getActives(queryParameters).getResults();

            if (routeCoverageResults.isEmpty()) {
                resultIsEmpty = true;
            } else {
                routeCoverageResultList.addAll(routeCoverageResults);
            }

        }

        return routeCoverageResultList;

    }

    private RouteCoveragePage getActives(Map<String, String> parameters) {

        HttpRequest request = HttpRequest.builder()
                .url(URL)
                .GET()
                .queryParams(parameters)
                .acceptedHttpStatuses(Set.of(OK))
                .build();

        return send(request, response ->
                response.getData(new TypeReference<>() {
                })
        );
    }

}
