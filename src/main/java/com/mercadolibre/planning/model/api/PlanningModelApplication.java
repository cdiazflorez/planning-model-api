package com.mercadolibre.planning.model.api;

import com.mercadolibre.planning.model.api.config.EnvironmentUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PlanningModelApplication {

    public static void main(final String[] args) {
        EnvironmentUtil.setup();
        SpringApplication.run(PlanningModelApplication.class, args);
    }
}
