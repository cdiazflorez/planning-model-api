package com.mercadolibre.planning.model.api.config;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Scope {
  DEVELOPMENT("development"),
  PROD_SLAVE("prod-slave"),
  PROD("prod"),
  TEST("test"),
  STAGE("stage");

  private final String springProfileName;

  public static Scope fromName(final String scopeName) {
    return scopeName == null ? DEVELOPMENT : scopeNameToScope(formatName(scopeName)).orElse(DEVELOPMENT);
  }

  private static String formatName(final String scopeName) {
    return scopeName
        .replace('-', '_')
        .toUpperCase(Locale.getDefault());
  }

  private static Optional<Scope> scopeNameToScope(final String scopeName) {
    return Stream.of(values())
        .filter(scope -> scopeName.startsWith(scope.name()))
        .findFirst();
  }
}
