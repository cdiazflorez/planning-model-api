package com.mercadolibre.planning.model.api.usecase.entities;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.MaxCapacityView;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.GetMaxCapacityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.getMockEntityCapacities;
import static com.mercadolibre.planning.model.api.util.TestUtils.getMockOutputCapacities;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetMaxCapacityViewEntityUseCaseTest {

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @InjectMocks
    private GetMaxCapacityEntityUseCase getMaxCapacityEntityUseCase;

    @Test
    @DisplayName("Get max capacity ok")
    public void testGetProductivityOk() {

        // GIVEN
        final List<MaxCapacityOutput> mockMaxCapacities = getMockOutputCapacities();
        final List<MaxCapacityView> mockMaxCapacitiesEntity = getMockEntityCapacities();

        when(processingDistRepository.findMaxCapacitiesByDateInRange(null,
                FBM_WMS_OUTBOUND.name(),
                A_DATE_UTC,
                A_DATE_UTC.plusDays(1))
        ).thenReturn(mockMaxCapacitiesEntity);

        // WHEN
        final List<MaxCapacityOutput> output = getMaxCapacityEntityUseCase.execute(
                FBM_WMS_OUTBOUND,
                A_DATE_UTC,
                A_DATE_UTC.plusDays(1));

        // THEN
        assertThat(output).isNotEmpty();
        assertEquals(3, output.size());
        assertEquals(output.get(0).maxCapacityValue, mockMaxCapacities.get(0).maxCapacityValue);
        assertEquals(output.get(1).maxCapacityValue, mockMaxCapacities.get(1).maxCapacityValue);
        assertEquals(output.get(2).maxCapacityValue, mockMaxCapacities.get(2).maxCapacityValue);
    }
}
