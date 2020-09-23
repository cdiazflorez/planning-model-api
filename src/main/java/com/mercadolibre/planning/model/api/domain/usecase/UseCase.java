package com.mercadolibre.planning.model.api.domain.usecase;

public interface UseCase<T, R> {
    R execute(T input);
}
