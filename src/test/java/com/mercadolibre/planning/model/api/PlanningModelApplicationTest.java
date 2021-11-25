package com.mercadolibre.planning.model.api;

import com.mercadolibre.planning.model.api.web.controller.PingController;
import com.mercadolibre.restclient.mock.RequestMockHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("functional")
@ActiveProfiles("development")
public class PlanningModelApplicationTest {

    @Autowired
    private PingController pingController;

    @BeforeEach
    public void setUp() {
        RequestMockHolder.clear();
    }

    @Test
    @DisplayName("Context Loads")
    public void contextLoads() {
        assertNotNull(pingController);
    }
}

