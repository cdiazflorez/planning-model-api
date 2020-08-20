package com.mercadolibre.planning.model.api.repository.forecast;

import com.mercadolibre.planning.model.api.dao.forecast.ForecastDao;
import com.mercadolibre.planning.model.api.dao.forecast.PlanningDistributionDao;
import com.mercadolibre.planning.model.api.dao.forecast.PlanningDistributionMetadataDao;
import com.mercadolibre.planning.model.api.dao.forecast.PlanningDistributionMetadataDaoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.PLANNING_METADATA_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.PLANNING_METADATA_VALUE;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDistMetadataDao;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDistribution;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecastDao;
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
        final ForecastDao forecastDao = mockSimpleForecastDao();
        entityManager.persistAndFlush(forecastDao);

        final PlanningDistributionDao planningDistribution = mockPlanningDistribution(forecastDao);
        entityManager.persistAndFlush(planningDistribution);

        final PlanningDistributionMetadataDao planningMetadata =
                mockPlanningDistMetadataDao(planningDistribution);

        entityManager.persistAndFlush(planningMetadata);

        // WHEN
        final Optional<PlanningDistributionMetadataDao> optPlanningMetadataDao = repository
                .findById(mockPlanningMetadataDaoId());

        // THEN
        assertTrue(optPlanningMetadataDao.isPresent());

        final PlanningDistributionMetadataDao foundPlanningMetadata = optPlanningMetadataDao.get();
        assertEquals(1L, foundPlanningMetadata.getPlanningDistributionId());
        assertEquals(PLANNING_METADATA_KEY, foundPlanningMetadata.getKey());
        assertEquals(PLANNING_METADATA_VALUE, foundPlanningMetadata.getValue());
    }

    @Test
    @DisplayName("Looking for a planning metadata that doesn't exist, returns nothing")
    public void testPlanningMetadataDoesntExist() {
        // WHEN
        final Optional<PlanningDistributionMetadataDao> optPlanningMetadataDao = repository
                .findById(mockPlanningMetadataDaoId());

        // THEN
        assertFalse(optPlanningMetadataDao.isPresent());
    }

    private PlanningDistributionMetadataDaoId mockPlanningMetadataDaoId() {
        return new PlanningDistributionMetadataDaoId(1L, PLANNING_METADATA_KEY);
    }
}
