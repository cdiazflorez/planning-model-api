package com.mercadolibre.planning.model.api.exception;

import static java.lang.String.format;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter;
import java.util.Arrays;

public class InvalidDomainFilterException extends RuntimeException {

    public static final String MESSAGE = "Domain %s only can use %s parameters";

    private final DomainType domainType;

    private final DomainOptionFilter[] domainOptionFilters;

    public InvalidDomainFilterException(final DomainType domainType, final DomainOptionFilter... domainOptionFilters){
        super();
        this.domainType = domainType;
        this.domainOptionFilters = domainOptionFilters;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE, domainType, Arrays.toString(domainOptionFilters));
    }
}
