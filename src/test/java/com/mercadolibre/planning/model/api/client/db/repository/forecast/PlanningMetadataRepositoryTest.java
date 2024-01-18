package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadataId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.PLANNING_METADATA_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.PLANNING_METADATA_VALUE;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDistMetadata;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PlanningMetadataRepositoryTest {

    @Autowired
    private PlanningMetadataRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Looking for a planning metadata that exists, returns it")
    public void testFindPlanningMetadataById() {
        // GIVEN
        final Forecast forecast = mockSimpleForecast();
        entityManager.persistAndFlush(forecast);

        final PlanningDistribution planningDistribution = mockPlanningDist(forecast);
        entityManager.persistAndFlush(planningDistribution);

        final PlanningDistributionMetadata planningMetadata =
                mockPlanningDistMetadata(planningDistribution);

        entityManager.persistAndFlush(planningMetadata);

        // WHEN
        final Optional<PlanningDistributionMetadata> optPlanningMetadata = repository
                .findById(new PlanningDistributionMetadataId(planningMetadata.getPlanningDistributionId(), planningMetadata.getKey()));

        // THEN
        assertTrue(optPlanningMetadata.isPresent());

        final PlanningDistributionMetadata foundPlanningMetadata = optPlanningMetadata.get();
        assertEquals(planningMetadata.getPlanningDistributionId(), foundPlanningMetadata.getPlanningDistributionId());
        assertEquals(planningMetadata.getKey(), foundPlanningMetadata.getKey());
        assertEquals(planningMetadata.getValue(), foundPlanningMetadata.getValue());
    }

    @Test
    @DisplayName("Looking for a planning metadata that doesn't exist, returns nothing")
    public void testPlanningMetadataDoesntExist() {
        // WHEN
        final Optional<PlanningDistributionMetadata> optPlanningMetadata = repository
                .findById(mockPlanningMetadataId());

        // THEN
        assertFalse(optPlanningMetadata.isPresent());
    }

    private PlanningDistributionMetadataId mockPlanningMetadataId() {
        return new PlanningDistributionMetadataId(1L, PLANNING_METADATA_KEY);
    }
}
