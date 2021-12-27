package com.mercadolibre.planning.model.api.domain.usecase.forecast.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastIdView;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

@SpringBootTest()
@AutoConfigureMockMvc
class GetForecastUseCaseTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @SneakyThrows
    public void testExecute() {
        // first request
        this.mvc
                .perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk());

        // second request
        this.mvc
                .perform(MockMvcRequestBuilders.get("/"))
                .andExpect(status().isOk());
    }

    @Getter
    @RequiredArgsConstructor
    private static class Fiv implements ForecastIdView {
        final Long id;
    }

    @TestConfiguration
    public static class TestConfig {
        static final String WAREHOUSE_ID = "H";
        static final ZonedDateTime DATE_FROM = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        static final ZonedDateTime DATE_TO = ZonedDateTime.of(2021, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC);

        @Bean
        @Primary
        public GetForecastUseCase.Repository getRepository() {
            return (warehouseId, workflow, weeks) -> List.of(
                    new Fiv((long)Workflow.valueOf(workflow).ordinal()),
                    new Fiv(2L)
            );
        }

        private List<Long> previousRequestForecastsIds;

        @Bean
        public RouterFunction<ServerResponse> getForecasts(final GetForecastUseCase getForecastUseCase) {
            return route().GET("/", req -> {
                // Verify that two calls to the `execute` method with same parameters from the same request scope
                // returns exactly the same object.
                var forecastsIds1 = getForecastUseCase.execute(new GetForecastInput(
                        WAREHOUSE_ID,
                        Workflow.FBM_WMS_OUTBOUND,
                        DATE_FROM,
                        DATE_TO
                ));
                var forecastsIds2 = getForecastUseCase.execute(new GetForecastInput(
                        WAREHOUSE_ID,
                        Workflow.FBM_WMS_OUTBOUND,
                        DATE_FROM,
                        DATE_TO
                ));
                assertSame(forecastsIds1, forecastsIds2);

                // Verify that two calls to the `execute` method with different parameters from the same request
                // scope returns different results.
                var forecastsIds3 = getForecastUseCase.execute(new GetForecastInput(
                        WAREHOUSE_ID,
                        Workflow.FBM_WMS_INBOUND,
                        DATE_FROM,
                        DATE_TO
                ));
                assertNotEquals(forecastsIds1, forecastsIds3);

                // Verify that calls to the `execute` method with same parameters but called from different requests
                // scopes return equivalent results but not exactly the same objects.
                assertNotSame(this.previousRequestForecastsIds, forecastsIds1);
                assertTrue(
                        this.previousRequestForecastsIds == null
                        || this.previousRequestForecastsIds.equals(forecastsIds1)
                );
                this.previousRequestForecastsIds = forecastsIds1;

                return ok().body(forecastsIds1);
            }).build();
        }
    }
}
