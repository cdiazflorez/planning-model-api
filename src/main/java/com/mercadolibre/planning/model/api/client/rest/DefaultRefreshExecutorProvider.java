package com.mercadolibre.planning.model.api.client.rest;

import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class DefaultRefreshExecutorProvider implements RouteCoverageClient.RefreshExecutorProvider {
    @Override
    public Executor getExecutor() {
        return Executors.newSingleThreadExecutor();
    }
}
