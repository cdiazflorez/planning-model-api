package com.mercadolibre.planning.model.api.exception;

import static java.lang.String.format;

import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter;
import java.util.Arrays;

public class InvalidInputFilterException extends RuntimeException {

    public static final String MESSAGE = "Input %s only can use %s parameters";

    private static final long serialVersionUID = 5800400356721253970L;

    private final InputId inputId;

    private final InputOptionFilter[] inputOptionFilters;

    public InvalidInputFilterException(final InputId inputId, final InputOptionFilter[] inputOptionFilters) {
        this.inputId = inputId;
        this.inputOptionFilters = inputOptionFilters;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE, inputId, Arrays.toString(inputOptionFilters));
    }
}
