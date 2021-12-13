package com.mercadolibre.planning.model.api.config;

import java.util.Optional;
import java.util.stream.Stream;

public enum Scope {
    DEVELOPMENT,
    PROD,
    PROD_SLAVE,
    TEST,
    STAGE;

    public static Scope fromName(final String scopeName) {
        if (scopeName == null) {
            return DEVELOPMENT;
        }

        return scopeNameToScope(formatName(scopeName)).orElse(DEVELOPMENT);
    }

    private static String formatName(final String scopeName) {
        return scopeName
                .replace('-', '_')
                .toUpperCase();
    }

    private static Optional<Scope> scopeNameToScope(final String scopeName) {
        return Stream.of(values())
                .filter(scope -> scopeName.startsWith(scope.name()))
                .findFirst();
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
