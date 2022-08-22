package com.mercadolibre.planning.model.api.client.db.repository.inputcatalog;

import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.ABSENCES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.BACKLOG_BOUNDS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.CONFIGURATION;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.CONTRACT_MODALITY_TYPES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.SHIFTS_PARAMETERS;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import com.mercadolibre.planning.model.api.usecase.inputcatalog.InputCatalogViewImpl;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InputCatalogRepositoryImplTest {

    @Mock
    private InputOptimizationJpaRepository inputOptimizationJpaRepository;

    private InputCatalogRepositoryImpl inputOptimizationRepository;

    @BeforeEach
    public void setUp() {
        inputOptimizationRepository = new InputCatalogRepositoryImpl(inputOptimizationJpaRepository);
    }

    @Test
    public void testFindInputOptimizationByWarehouse() throws IOException {
        // GIVEN
        final Set<InputCatalogView> expectedInputCatalogViews = Set.of(
                new InputCatalogViewImpl(ABSENCES, getResourceAsString("inputcatalog/inputs/absences.json")),
                new InputCatalogViewImpl(BACKLOG_BOUNDS, getResourceAsString("inputcatalog/inputs/backlog_bounds.json")),
                new InputCatalogViewImpl(CONFIGURATION, getResourceAsString("inputcatalog/inputs/configuration.json")),
                new InputCatalogViewImpl(CONTRACT_MODALITY_TYPES, getResourceAsString(
                        "inputcatalog/inputs/contract_modality_type.json"))
        );
        when(inputOptimizationJpaRepository.findAllByWarehouseId(WAREHOUSE_ID)).thenReturn(expectedInputCatalogViews);
        // WHEN
        final Map<InputId, String> result = inputOptimizationRepository.getInputs(WAREHOUSE_ID, Set.of());
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
        final Set<InputCatalogView> expectedInputCatalogViews = Set.of(
                new InputCatalogViewImpl(CONFIGURATION, getResourceAsString("inputcatalog/inputs/configuration.json"))
        );
        when(inputOptimizationJpaRepository.findAllByWarehouseIdAndDomainIn(WAREHOUSE_ID, Set.of(CONFIGURATION)))
                .thenReturn(expectedInputCatalogViews);
        // WHEN
        final Map<InputId, String> result = inputOptimizationRepository.getInputs(WAREHOUSE_ID, Set.of(CONFIGURATION));
        // THEN
        assertFalse(result.containsKey(ABSENCES));
        assertFalse(result.containsKey(BACKLOG_BOUNDS));
        assertTrue(result.containsKey(CONFIGURATION));
        assertFalse(result.containsKey(CONTRACT_MODALITY_TYPES));
    }

}
