package com.mercadolibre.planning.model.api.client.rest;

import org.springframework.stereotype.Service;

@Service
public class NanoTimeService {
    public long getNanoTime() {
        return System.nanoTime();
    }
}
