package com.mercadolibre.planning.model.api.domain.entity;

public interface WorkflowService<T, V> {
    V executeInbound(T params);

    V executeOutbound(T params);
}
