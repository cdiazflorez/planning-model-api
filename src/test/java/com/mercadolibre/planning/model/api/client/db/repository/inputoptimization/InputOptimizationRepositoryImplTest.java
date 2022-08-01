package com.mercadolibre.planning.model.api.client.db.repository.inputoptimization;

import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.ABSENCES;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.BACKLOG_BOUNDS;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.CONFIGURATION;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.CONTRACT_MODALITY_TYPES;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.SHIFTS_PARAMETERS;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.usecase.inputoptimization.InputOptimizationViewImpl;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InputOptimizationRepositoryImplTest {

    @Mock
    private InputOptimizationJpaRepository inputOptimizationJpaRepository;

    private InputOptimizationRepositoryImpl inputOptimizationRepository;

    @BeforeEach
    public void setUp() {
        inputOptimizationRepository = new InputOptimizationRepositoryImpl(inputOptimizationJpaRepository);
    }

    @Test
    public void testFindInputOptimizationByWarehouse() throws IOException {
        // GIVEN
        final Set<InputOptimizationView> expectedInputOptimizationViews = Set.of(
                new InputOptimizationViewImpl(ABSENCES, getResourceAsString("inputoptimization/domain/absences.json")),
                new InputOptimizationViewImpl(BACKLOG_BOUNDS, getResourceAsString("inputoptimization/domain/backlog_bounds.json")),
                new InputOptimizationViewImpl(CONFIGURATION, getResourceAsString("inputoptimization/domain/configuration.json")),
                new InputOptimizationViewImpl(CONTRACT_MODALITY_TYPES, getResourceAsString("inputoptimization/domain/contract_modality_type.json"))
        );
        when(inputOptimizationJpaRepository.findAllByWarehouseId(WAREHOUSE_ID)).thenReturn(expectedInputOptimizationViews);
        // WHEN
        final Map<DomainType, String> result = inputOptimizationRepository.getInputs(WAREHOUSE_ID, Set.of());
        // THEN
        assertTrue(result.containsKey(ABSENCES));
        assertTrue(result.containsKey(BACKLOG_BOUNDS));
        assertTrue(result.containsKey(CONFIGURATION));
        assertTrue(result.containsKey(CONTRACT_MODALITY_TYPES));
        assertFalse(result.containsKey(SHIFTS_PARAMETERS));
    }

    @Test
    public void testFindInputOptimizationByWarehouseAndDomain() throws IOException {
        // GIVEN
        final Set<InputOptimizationView> expectedInputOptimizationViews = Set.of(
                new InputOptimizationViewImpl(CONFIGURATION, getResourceAsString("inputoptimization/domain/configuration.json"))
        );
        when(inputOptimizationJpaRepository.findAllByWarehouseIdAndDomainIn(WAREHOUSE_ID, Set.of(CONFIGURATION)))
                .thenReturn(expectedInputOptimizationViews);
        // WHEN
        final Map<DomainType, String> result = inputOptimizationRepository.getInputs(WAREHOUSE_ID, Set.of(CONFIGURATION));
        // THEN
        assertFalse(result.containsKey(ABSENCES));
        assertFalse(result.containsKey(BACKLOG_BOUNDS));
        assertTrue(result.containsKey(CONFIGURATION));
        assertFalse(result.containsKey(CONTRACT_MODALITY_TYPES));
    }

}
