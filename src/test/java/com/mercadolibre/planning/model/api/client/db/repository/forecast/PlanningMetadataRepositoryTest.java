package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadataEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionMetadataEntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
        final ForecastEntity forecastEntity = mockSimpleForecast();
        entityManager.persistAndFlush(forecastEntity);

        final PlanningDistributionEntity planningDistribution = mockPlanningDist(forecastEntity);
        entityManager.persistAndFlush(planningDistribution);

        final PlanningDistributionMetadataEntity planningMetadata =
                mockPlanningDistMetadata(planningDistribution);

        entityManager.persistAndFlush(planningMetadata);

        // WHEN
        final Optional<PlanningDistributionMetadataEntity> optPlanningMetadata = repository
                .findById(mockPlanningMetadataId());

        // THEN
        assertTrue(optPlanningMetadata.isPresent());

        final PlanningDistributionMetadataEntity foundPlanningMetadata = optPlanningMetadata.get();
        assertEquals(1L, foundPlanningMetadata.getPlanningDistributionId());
        assertEquals(PLANNING_METADATA_KEY, foundPlanningMetadata.getKey());
        assertEquals(PLANNING_METADATA_VALUE, foundPlanningMetadata.getValue());
    }

    @Test
    @DisplayName("Looking for a planning metadata that doesn't exist, returns nothing")
    public void testPlanningMetadataDoesntExist() {
        // WHEN
        final Optional<PlanningDistributionMetadataEntity> optPlanningMetadata = repository
                .findById(mockPlanningMetadataId());

        // THEN
        assertFalse(optPlanningMetadata.isPresent());
    }

    private PlanningDistributionMetadataEntityId mockPlanningMetadataId() {
        return new PlanningDistributionMetadataEntityId(1L, PLANNING_METADATA_KEY);
    }
}
