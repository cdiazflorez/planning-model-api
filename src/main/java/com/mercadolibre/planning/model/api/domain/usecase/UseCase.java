package com.mercadolibre.planning.model.api.domain.usecase;

@Deprecated
public interface UseCase<T, R> {
    R execute(T input);
}
