package com.mercadolibre.planning.model.api.config;

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.env.AbstractEnvironment;

public class EnvironmentUtilTest {

    @AfterAll
    static void resetEnvironment() {
        EnvironmentUtil.setup("development");
    }

  @ParameterizedTest
  @MethodSource("scopeNamesAndProfiles")
  public void testProd(final String furyScopeName, final String springProfile) {

    // When
    EnvironmentUtil.setup(furyScopeName);

    // Then
    final String selectedProfile = System.getProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME);
    Assertions.assertEquals(springProfile, selectedProfile);
  }

  private static Stream<Arguments> scopeNamesAndProfiles() {
    return Stream.of(
        Arguments.of("prod", "prod"),
        Arguments.of("prod-2", "prod"),
        Arguments.of("bla", "development"),
        Arguments.of("prod-slave", "prod-slave"),
        Arguments.of("stage", "stage"),
        Arguments.of("test", "test")
    );
  }
}
