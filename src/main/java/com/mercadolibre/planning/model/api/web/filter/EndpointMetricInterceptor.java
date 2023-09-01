package com.mercadolibre.planning.model.api.web.filter;

import com.google.common.base.CaseFormat;
import com.mercadolibre.metrics.Metrics;
import com.mercadolibre.planning.model.api.exception.InvalidPathException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * EndpointMetricInterceptor metrics which app calls which endpoint.
 * It uses Fury's Traffic Layer headers to get the client name,
 * and Spring Servlet handler to get the pattern of the url being called.
 * Metrics are sent to datadog.
 * If the application name cannot be inferred, it is set as "unknown".
 * Path patterns are sanitized to remove "{}" and placeholders.
 * @see <a href="https://docs.datadoghq.com/developers/guide/what-best-practices-are-recommended-for-naming-metrics-and-tags/#rules-and-best-practices-for-naming-tags">Datadog's best practices for tags</a>
 */
@Slf4j
@Component
public class EndpointMetricInterceptor implements HandlerInterceptor {

    protected static final String DEFAULT_CLIENT = "unknown";
    // TODO get app name from somewhere, so this component can be reused
    protected static final String METRIC_NAME = "application.planning.model.api.endpoint_metric";
    protected static final String REQUEST_HEADER_CLIENT_APPLICATION = "X-Api-Client-Application";

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object object) {
        try {
            metricHandle(request);
        } catch (Exception e) {
            log.warn("endpoint metric not recorded, request will continue: {}", e.getMessage());
        }

        return true;
    }

    private void metricHandle(final HttpServletRequest request) {
        final String clientName = getClientName(request);
        final String path = getCleanPath((String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        final String method = request.getMethod();

        Metrics.INSTANCE.increment(METRIC_NAME, getMetricTags(clientName, path, method));
    }

    protected String getClientName(final HttpServletRequest request) {
        final String client = request.getHeader(REQUEST_HEADER_CLIENT_APPLICATION);
        return client != null ? client : DEFAULT_CLIENT;
    }

    private static String[] getMetricTags(final String clientName, final String path, final String method) {
        return new String[]{
                "client_application:" + clientName,
                "path:" + path,
                "method:" + method,
        };
    }

    protected String getCleanPath(final String path) {
        if (path == null) {
            throw new InvalidPathException();
        }

        String clean = path.replaceAll("[{}]", "");
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, clean);
    }

}
