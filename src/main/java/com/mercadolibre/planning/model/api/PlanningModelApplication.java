package com.mercadolibre.planning.model.api;

import com.mercadolibre.planning.model.api.config.EnvironmentUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PlanningModelApplication {

  private static final String SCOPE_ENV_VARIABLE = "SCOPE";

  public static void main(final String[] args) {

    EnvironmentUtil.setup(System.getenv(SCOPE_ENV_VARIABLE));
    SpringApplication.run(PlanningModelApplication.class, args);
  }
}
