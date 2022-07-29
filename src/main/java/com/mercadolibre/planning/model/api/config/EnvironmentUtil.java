package com.mercadolibre.planning.model.api.config;

import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.AbstractEnvironment;

@Slf4j
public final class EnvironmentUtil {

  private EnvironmentUtil() {
  }

  public static void setup(final String furyScopeName) {
    final Scope scope = Scope.fromName(furyScopeName);

    log.info("Fury scope name: {}", furyScopeName);
    log.info("Current scope: {}", scope);

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, scope.getSpringProfileName());
  }

}
