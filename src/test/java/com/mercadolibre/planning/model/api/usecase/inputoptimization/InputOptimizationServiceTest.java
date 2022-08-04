package com.mercadolibre.planning.model.api.usecase.inputoptimization;

import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.ABSENCES;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.BACKLOG_BOUNDS;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.CONFIGURATION;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.CONTRACT_MODALITY_TYPES;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.NON_SYSTEMIC_RATIO;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.POLYVALENCE_PARAMETERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.PRESENCES;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.SHIFTS_PARAMETERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.SHIFT_CONTRACT_MODALITIES;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.TRANSFERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.WORKERS_PARAMETERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType.WORKER_COSTS;
import static com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter.INCLUDE_DAY_NAME;
import static com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter.INCLUDE_PROCESS;
import static com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter.INCLUDE_SHIFT_GROUP;
import static com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.inputdomain.DomainOptionFilter.INCLUDE_SUB_PROCESS;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mercadolibre.json.JsonUtils;
import com.mercadolibre.json_jackson.JsonJackson;
import com.mercadolibre.planning.model.api.domain.entity.inputoptimization.DomainType;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.InputOptimizationService;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.InputOptimizationService.InputOptimizationRepository;
import com.mercadolibre.planning.model.api.domain.usecase.inputoptimization.get.GetInputOptimization;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InputOptimizationServiceTest {

    @Mock
    private InputOptimizationRepository inputOptimizationRepository;

    private InputOptimizationService inputOptimizationService;

    @BeforeEach
    public void setUp() {
        inputOptimizationService = new InputOptimizationService(objectMapper(), inputOptimizationRepository);
    }

    @Test
    public void allInputsOptimizationTest() throws IOException {
        //GIVEN
        final Map<DomainType, String> domainResults = new LinkedHashMap<>();
        domainResults.put(ABSENCES, getResourceAsString("inputoptimization/domain/absences.json"));
        domainResults.put(BACKLOG_BOUNDS, getResourceAsString("inputoptimization/domain/backlog_bounds.json"));
        domainResults.put(CONFIGURATION, getResourceAsString("inputoptimization/domain/configuration.json"));
        domainResults.put(CONTRACT_MODALITY_TYPES, getResourceAsString("inputoptimization/domain/contract_modality_type.json"));
        domainResults.put(NON_SYSTEMIC_RATIO, getResourceAsString("inputoptimization/domain/non_systemic_ratio.json"));
        domainResults.put(POLYVALENCE_PARAMETERS, getResourceAsString("inputoptimization/domain/polyvalence_parameters.json"));
        domainResults.put(PRESENCES, getResourceAsString("inputoptimization/domain/presences.json"));
        domainResults.put(SHIFT_CONTRACT_MODALITIES, getResourceAsString("inputoptimization/domain/shift_contract_modality.json"));
        domainResults.put(SHIFTS_PARAMETERS, getResourceAsString("inputoptimization/domain/shift_parameters.json"));
        domainResults.put(TRANSFERS, getResourceAsString("inputoptimization/domain/transfers.json"));
        domainResults.put(WORKER_COSTS, getResourceAsString("inputoptimization/domain/worker_costs.json"));
        domainResults.put(WORKERS_PARAMETERS, getResourceAsString("inputoptimization/domain/workers_parameters.json"));

        final GetInputOptimization getInputOptimization = new GetInputOptimization(WAREHOUSE_ID, Map.of());

        when(inputOptimizationRepository.getInputs(getInputOptimization.getWarehouseId(), Set.of())).thenReturn(
                domainResults);

        //WHEN
        Map<DomainType, Object> result = inputOptimizationService.getInputOptimization(getInputOptimization);
        //THEN
        assertEquals(12, result.size());
        assertTrue(result.containsKey(ABSENCES));
        assertTrue(result.containsKey(BACKLOG_BOUNDS));
        assertTrue(result.containsKey(CONFIGURATION));
        assertTrue(result.containsKey(CONTRACT_MODALITY_TYPES));
        assertTrue(result.containsKey(NON_SYSTEMIC_RATIO));
        assertTrue(result.containsKey(POLYVALENCE_PARAMETERS));
        assertTrue(result.containsKey(SHIFT_CONTRACT_MODALITIES));
        assertTrue(result.containsKey(SHIFTS_PARAMETERS));
        assertTrue(result.containsKey(TRANSFERS));
        assertTrue(result.containsKey(WORKER_COSTS));
        assertTrue(result.containsKey(WORKERS_PARAMETERS));

    }

    @ParameterizedTest
    @MethodSource("domainFilterArguments")
    public void someInputsOptimizationTest(final Map<DomainType, Map<String, List<Object>>> domainFilters) throws IOException {
        //GIVEN
        final Map<DomainType, String> domainResults = Map.of(
                NON_SYSTEMIC_RATIO, getResourceAsString("inputoptimization/response_with_filters/non_systemic_ratio.json"),
                SHIFTS_PARAMETERS, getResourceAsString("inputoptimization/response_with_filters/shift_parameters.json")
        );

        final GetInputOptimization getInputOptimization = new GetInputOptimization(WAREHOUSE_ID, domainFilters);

        when(inputOptimizationRepository.getInputs(getInputOptimization.getWarehouseId(), getInputOptimization.getDomains().keySet()))
                .thenReturn(domainResults);

        //WHEN
        Map<DomainType, Object> result = inputOptimizationService.getInputOptimization(getInputOptimization);

        //THEN
        assertEquals(2, result.size());
        assertTrue(result.containsKey(NON_SYSTEMIC_RATIO));
        assertTrue(result.containsKey(SHIFTS_PARAMETERS));
    }

    private static Stream<Arguments> domainFilterArguments() {
        return Stream.of(
                Arguments.of(Map.of()),
                Arguments.of(Map.of(
                        SHIFTS_PARAMETERS, Map.of(
                                INCLUDE_DAY_NAME.toJson(), List.of("Mon", "Thu"),
                                INCLUDE_SHIFT_GROUP.toJson(), List.of("AFTERNOON01")
                        ),
                        NON_SYSTEMIC_RATIO, Map.of(
                                INCLUDE_PROCESS.toJson(), List.of("fbm_wms_inbound"),
                                INCLUDE_SUB_PROCESS.toJson(), List.of("put_away")
                        )
                ))
        );
    }


    public ObjectMapper objectMapper() {
        return ((JsonJackson) JsonUtils.INSTANCE.getEngine())
                .getMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

}
