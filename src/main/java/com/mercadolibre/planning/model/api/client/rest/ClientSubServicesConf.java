package com.mercadolibre.planning.model.api.client.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ClientSubServicesConf {
    @Bean
    public ExecutorService getRefreshExecutor() {
        return Executors.newSingleThreadExecutor();
    }
}
