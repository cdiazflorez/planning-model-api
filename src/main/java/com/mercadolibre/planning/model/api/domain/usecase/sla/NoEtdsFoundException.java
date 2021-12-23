package com.mercadolibre.planning.model.api.domain.usecase.sla;

public class NoEtdsFoundException extends RuntimeException {
    public NoEtdsFoundException(final String message) {
        super(message);
    }
}
