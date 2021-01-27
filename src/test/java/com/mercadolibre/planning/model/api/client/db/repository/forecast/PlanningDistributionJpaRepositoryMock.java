package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import javax.persistence.EntityManager;

public class PlanningDistributionJpaRepositoryMock extends PlanningDistributionJpaRepository {

    public PlanningDistributionJpaRepositoryMock(final EntityManager entityManager) {
        super(entityManager);
    }

    @Override
    protected String getPlanningDistributionMetadataQuery(final int size) {
        return super.getPlanningDistributionMetadataQuery(size).replace('`', '\"');
    }
}
