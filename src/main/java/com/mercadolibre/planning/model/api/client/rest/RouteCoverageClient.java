package com.mercadolibre.planning.model.api.client.rest;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.api.client.rest.config.RestPool;
import com.mercadolibre.planning.model.api.domain.entity.sla.RouteCoveragePage;
import com.mercadolibre.planning.model.api.domain.entity.sla.RouteCoverageResult;
import com.mercadolibre.planning.model.api.gateway.RouteCoverageClientGateway;
import com.mercadolibre.restclient.MeliRestClient;
import com.newrelic.api.agent.Trace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
public class RouteCoverageClient extends HttpClient implements RouteCoverageClientGateway {

    private static final long MAX_AGE = 60L * 60L * 1_000_000_000L;
    private static final String URL = "/shipping/routes/rules";
    private static final Map<String, String> SITE_BY_WAREHOUSE_PREFIX = Map.of(
            "AR", "MLA",
            "MX", "MLM",
            "CL", "MLC",
            "CO", "MCO",
            "BR", "MLB"
    );

    private final Executor refreshExecutor;

    private final ConcurrentMap<String, Record> cache = new ConcurrentHashMap<>();

    private final NanoTimeService nanoTimeService;

    public RouteCoverageClient(final MeliRestClient client,
                               final NanoTimeService nanoTimeService,
                               final RefreshExecutorProvider refreshExecutorProvider) {
        super(client, RestPool.ROUTE_COVERAGE.name());
        this.nanoTimeService = nanoTimeService;
        this.refreshExecutor = refreshExecutorProvider.getExecutor();
    }

    @RequiredArgsConstructor
    private static class Record {
        public final List<RouteCoverageResult> value;
        public final long expirationNano;
    }

    @Trace
    @Override
    /**Si el get no se ejecuta por varias horas, estariamos devolviendo una respuesta antigua a causa del cache
     * se asume que esto no ocurre a causa de que flowmonitor reliza llamadas reiterativas que son menores al tiempo
     * del  {@link #MAX_AGE}**/
    public List<RouteCoverageResult> get(final String warehouse) {
        final long currentNano = nanoTimeService.getNanoTime();

        final var hit = cache.get(warehouse);
        if (hit == null) {
            final var value = load(warehouse);
            cache.put(warehouse, new Record(value, currentNano + MAX_AGE));
            return value;
        } else {
            if (currentNano >= hit.expirationNano) {
                refresh(warehouse);
            }
            return hit.value;
        }
    }

    private void refresh(final String warehouse) {
        final var attrs = RequestContextHolder.getRequestAttributes();
        CompletableFuture.runAsync(
                () -> {
                    RequestContextHolder.setRequestAttributes(attrs);
                    final var value = load(warehouse);
                    cache.put(warehouse, new Record(value, System.nanoTime() + MAX_AGE));
                },
                refreshExecutor
        );
    }

    private List<RouteCoverageResult> load(final String warehouse) {
        final Map<String, String> queryParameters = new HashMap<>();

        queryParameters.put("status", "active");
        queryParameters.put("site", SITE_BY_WAREHOUSE_PREFIX.get(warehouse.substring(0, 2)));
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

    private RouteCoveragePage getActives(final Map<String, String> parameters) {

        final HttpRequest request = HttpRequest.builder()
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

    public interface RefreshExecutorProvider {
        Executor getExecutor();
    }

}
