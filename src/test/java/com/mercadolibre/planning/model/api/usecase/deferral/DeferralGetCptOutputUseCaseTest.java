package com.mercadolibre.planning.model.api.usecase.deferral;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralCptUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralDto;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.DeferralInput;
import com.mercadolibre.planning.model.api.domain.usecase.deferral.ProjectionDto;
import com.mercadolibre.planning.model.api.gateway.DeferralGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeferralGetCptOutputUseCaseTest {

    private static final String WAREHOUSE_ID = "ARTW01";

    private static final ZonedDateTime CPT_1 = ZonedDateTime.now();

    private static final ZonedDateTime CPT_2 = ZonedDateTime.now().plusHours(1);

    @InjectMocks
    private DeferralCptUseCase useCase;

    @Mock
    private DeferralGateway deferralGateway;

    @Mock
    private CurrentPlanningDistributionRepository currentPlanningRepository;

    @Test
    public void testExecuteShouldDeferralAndCurrentNotExists() {
        //GIVEN
        when(deferralGateway.getDeferralProjection(WAREHOUSE_ID, FBM_WMS_OUTBOUND)).thenReturn(
                new DeferralDto(WAREHOUSE_ID, FBM_WMS_OUTBOUND.name(), List.of(
                        new ProjectionDto(CPT_1, true),
                        new ProjectionDto(CPT_2, true)
                )));
        when(currentPlanningRepository
                .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                        FBM_WMS_OUTBOUND, WAREHOUSE_ID, CPT_1, CPT_2))
                .thenReturn(emptyList());

        //WHEN
        final List<CurrentPlanningDistribution> response = useCase.execute(
                new DeferralInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND));

        //THEN
        assertEquals(2, response.size());

        assertEquals(CPT_1, response.get(0).getDateOut());
        assertEquals(0, response.get(0).getQuantity());
        assertTrue(response.get(0).isActive());

        assertEquals(CPT_2, response.get(1).getDateOut());
        assertEquals(0, response.get(1).getQuantity());
        assertTrue(response.get(1).isActive());
    }

    @Test
    public void testExecuteShouldDeferralAndCurrentExists() {
        //GIVEN
        when(deferralGateway.getDeferralProjection(WAREHOUSE_ID, FBM_WMS_OUTBOUND)).thenReturn(
                new DeferralDto(WAREHOUSE_ID, FBM_WMS_OUTBOUND.name(), List.of(
                        new ProjectionDto(CPT_2, true)
                )));
        when(currentPlanningRepository
                .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                        FBM_WMS_OUTBOUND, WAREHOUSE_ID, CPT_2, CPT_2))
                .thenReturn(List.of(CurrentPlanningDistribution.builder()
                        .dateOut(CPT_2)
                        .isActive(false)
                        .build()));

        //WHEN
        final List<CurrentPlanningDistribution> response = useCase.execute(
                new DeferralInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND));

        //THEN
        assertEquals(1, response.size());

        assertEquals(CPT_2, response.get(0).getDateOut());
        assertEquals(0, response.get(0).getQuantity());
        assertTrue(response.get(0).isActive());
    }

    @Test
    public void testExecuteShouldNotDeferralAndCurrentExists() {
        //GIVEN
        when(deferralGateway.getDeferralProjection(WAREHOUSE_ID, FBM_WMS_OUTBOUND)).thenReturn(
                new DeferralDto(WAREHOUSE_ID, FBM_WMS_OUTBOUND.name(), List.of(
                        new ProjectionDto(CPT_1, false),
                        new ProjectionDto(CPT_2, false)
                )));
        when(currentPlanningRepository
                .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                        FBM_WMS_OUTBOUND, WAREHOUSE_ID, CPT_1, CPT_2))
                .thenReturn(List.of(CurrentPlanningDistribution.builder()
                        .dateOut(CPT_2)
                        .isActive(true)
                        .build()));

        //WHEN
        final List<CurrentPlanningDistribution> response = useCase.execute(
                new DeferralInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND));

        //THEN
        assertEquals(1, response.size());

        assertEquals(CPT_2, response.get(0).getDateOut());
        assertEquals(0, response.get(0).getQuantity());
        assertFalse(response.get(0).isActive());
    }

    @Test
    public void testExecuteShouldUpdateDB() {
        //GIVEN
        when(deferralGateway.getDeferralProjection(WAREHOUSE_ID, FBM_WMS_OUTBOUND)).thenReturn(
                new DeferralDto(WAREHOUSE_ID, FBM_WMS_OUTBOUND.name(), emptyList()));

        //WHEN
        final List<CurrentPlanningDistribution> response = useCase.execute(
                new DeferralInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND));

        //THEN
        assertEquals(0, response.size());
        verifyZeroInteractions(currentPlanningRepository);
    }
}
