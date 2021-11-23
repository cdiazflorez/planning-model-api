package com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse;

public class NoEtdsFoundException extends RuntimeException {
    public NoEtdsFoundException(final String message) {
        super(message);
    }
}
