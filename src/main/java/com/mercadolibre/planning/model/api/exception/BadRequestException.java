package com.mercadolibre.planning.model.api.exception;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@Value
public class BadRequestException extends RuntimeException {

    private String message;
}
