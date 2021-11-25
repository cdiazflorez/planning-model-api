package com.mercadolibre.planning.model.api.client.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mercadolibre.json.JsonUtils;
import com.mercadolibre.json_jackson.JsonJackson;
import com.mercadolibre.planning.model.api.client.rest.config.RestClientConfig;
import com.mercadolibre.restclient.MeliRestClient;
import com.mercadolibre.restclient.mock.RequestMockHolder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class BaseClientTest {

    protected static final String BASE_URL_ROUTE = "http://internal-api.mercadolibre.com/shipping/route";

    protected MeliRestClient getRestTestClient() throws IOException {

        final RestClientConfig.RouteEtsClientProperties routeEtsClientProperties =
                new RestClientConfig.RouteEtsClientProperties();
        routeEtsClientProperties.setBaseUrl(BASE_URL_ROUTE);

        return new RestClientConfig(
                routeEtsClientProperties
        ).restClient();
    }

    public void cleanMocks() {
        RequestMockHolder.clear();
    }

    public String getResourceAsString(final String resourceName) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream resource = classLoader.getResourceAsStream(resourceName)) {
            return IOUtils.toString(resource, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public ObjectMapper objectMapper() {
        return ((JsonJackson) JsonUtils.INSTANCE.getEngine())
                .getMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
