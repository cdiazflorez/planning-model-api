package com.mercadolibre.planning.model.api.config;

import com.mercadolibre.routing.RoutingFilter;
import com.mercadolibre.threading.executor.MeliExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.concurrent.ExecutorService;

@Configuration
public class MeliContextFilterConfig {

    @Bean
    @Order(1)
    public RoutingFilter getRoutingFilter() {
        return new RoutingFilter();
    }

    @Bean
    public ExecutorService meliContextAwareExecutorService() {
        return MeliExecutors.newFixedThreadPool(1);
    }

}
