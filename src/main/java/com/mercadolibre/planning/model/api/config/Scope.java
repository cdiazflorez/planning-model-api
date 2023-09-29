package com.mercadolibre.planning.model.api.config;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The scope of pre-prod points to stage because for now we only want to test waverless,
 * in the future a complete pre-prod environment can be generated.
 */
@Getter
@AllArgsConstructor
public enum Scope {
  DEVELOPMENT("development"),
  PROD_SLAVE("prod-slave"),
  PROD("prod"),
  TEST("test"),
  STAGE("stage"),
  PRE_PROD("stage");

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
