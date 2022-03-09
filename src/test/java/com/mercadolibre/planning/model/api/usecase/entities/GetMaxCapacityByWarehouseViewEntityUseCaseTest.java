package com.mercadolibre.planning.model.api.usecase.entities;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getMockEntityCapacities;
import static com.mercadolibre.planning.model.api.util.TestUtils.getMockOutputCapacities;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.MaxCapacityView;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.GetMaxCapacityByWarehouseEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityOutput;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetMaxCapacityByWarehouseViewEntityUseCaseTest {

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @InjectMocks
    private GetMaxCapacityByWarehouseEntityUseCase getMaxCapacityByWarehouseEntityUseCase;

    @Test
    @DisplayName("Get tph max capacity ok")
    public void testGetProductivityOk() {

        // GIVEN
        final List<MaxCapacityOutput> mockMaxCapacities = getMockOutputCapacities();
        final List<MaxCapacityView> mockMaxCapacitiesEntity = getMockEntityCapacities();

        when(processingDistRepository.findMaxCapacitiesByWarehouseAndDateInRange(
                WAREHOUSE_ID,
                A_DATE_UTC,
                A_DATE_UTC.plusDays(72))
        ).thenReturn(mockMaxCapacitiesEntity);

        // WHEN
        final List<MaxCapacityOutput> output = getMaxCapacityByWarehouseEntityUseCase.execute(
                WAREHOUSE_ID,
                A_DATE_UTC,
                A_DATE_UTC.plusDays(72));

        // THEN
        assertThat(output).isNotEmpty();
        assertEquals(3, output.size());
        assertEquals(output.get(0).maxCapacityValue, mockMaxCapacities.get(0).maxCapacityValue);
        assertEquals(output.get(1).maxCapacityValue, mockMaxCapacities.get(1).maxCapacityValue);
        assertEquals(output.get(2).maxCapacityValue, mockMaxCapacities.get(2).maxCapacityValue);
    }
}
