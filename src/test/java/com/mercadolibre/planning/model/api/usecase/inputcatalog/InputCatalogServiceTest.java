package com.mercadolibre.planning.model.api.usecase.inputcatalog;

import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.ABSENCES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.BACKLOG_BOUNDS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.CONFIGURATION;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.CONTRACT_MODALITY_TYPES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.NON_SYSTEMIC_RATIO;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.POLYVALENCE_PARAMETERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.PRESENCES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.SHIFTS_PARAMETERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.SHIFT_CONTRACT_MODALITIES;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.TRANSFERS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.WORKERS_COSTS;
import static com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId.WORKERS_PARAMETERS;
import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_DAY_NAME;
import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_PROCESS;
import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_SHIFT_GROUP;
import static com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.inputdomain.InputOptionFilter.INCLUDE_STAGE;

import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.getResourceAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mercadolibre.json.JsonUtils;
import com.mercadolibre.json_jackson.JsonJackson;
import com.mercadolibre.planning.model.api.domain.entity.inputcatalog.InputId;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.InputCatalogService;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.InputCatalogService.InputCatalogRepository;
import com.mercadolibre.planning.model.api.domain.usecase.inputcatalog.get.GetInputCatalog;
import com.mercadolibre.planning.model.api.exception.InvalidInputFilterException;
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
public class InputCatalogServiceTest {

  @Mock
  private InputCatalogRepository inputCatalogRepository;

  private InputCatalogService inputCatalogService;

  @BeforeEach
  public void setUp() {
    inputCatalogService = new InputCatalogService(objectMapper(), inputCatalogRepository);
  }

  @Test
  public void allInputsOptimizationTest() throws IOException {
    //GIVEN
    final Map<InputId, String> domainResults = new LinkedHashMap<>();
    domainResults.put(ABSENCES, getResourceAsString("inputcatalog/inputs/absences.json"));
    domainResults.put(BACKLOG_BOUNDS, getResourceAsString("inputcatalog/inputs/backlog_bounds.json"));
    domainResults.put(CONFIGURATION, getResourceAsString("inputcatalog/inputs/configuration.json"));
    domainResults.put(CONTRACT_MODALITY_TYPES, getResourceAsString("inputcatalog/inputs/contract_modality_type.json"));
    domainResults.put(NON_SYSTEMIC_RATIO, getResourceAsString("inputcatalog/inputs/non_systemic_ratio.json"));
    domainResults.put(POLYVALENCE_PARAMETERS, getResourceAsString("inputcatalog/inputs/polyvalence_parameters.json"));
    domainResults.put(PRESENCES, getResourceAsString("inputcatalog/inputs/presences.json"));
    domainResults.put(SHIFT_CONTRACT_MODALITIES, getResourceAsString("inputcatalog/inputs/shift_contract_modality.json"));
    domainResults.put(SHIFTS_PARAMETERS, getResourceAsString("inputcatalog/inputs/shift_parameters.json"));
    domainResults.put(TRANSFERS, getResourceAsString("inputcatalog/inputs/transfers.json"));
    domainResults.put(WORKERS_COSTS, getResourceAsString("inputcatalog/inputs/worker_costs.json"));
    domainResults.put(WORKERS_PARAMETERS, getResourceAsString("inputcatalog/inputs/workers_parameters.json"));

    final GetInputCatalog getInputCatalog = new GetInputCatalog(WAREHOUSE_ID, Map.of());

    when(inputCatalogRepository.getInputs(getInputCatalog.getWarehouseId(), Set.of())).thenReturn(
        domainResults);

    //WHEN
    Map<InputId, Object> result = inputCatalogService.getInputsCatalog(getInputCatalog);
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
    assertTrue(result.containsKey(WORKERS_COSTS));
    assertTrue(result.containsKey(WORKERS_PARAMETERS));

  }

  @ParameterizedTest
  @MethodSource("domainFilterArguments")
  public void someInputsOptimizationTest(final Map<InputId, Map<String, List<Object>>> domainFilters) throws IOException {
    //GIVEN
    final Map<InputId, String> domainResults = Map.of(
        NON_SYSTEMIC_RATIO, getResourceAsString("inputcatalog/response_with_filters/non_systemic_ratio.json"),
        SHIFTS_PARAMETERS, getResourceAsString("inputcatalog/response_with_filters/shift_parameters.json")
    );

    final GetInputCatalog getInputCatalog = new GetInputCatalog(WAREHOUSE_ID, domainFilters);

    when(inputCatalogRepository.getInputs(getInputCatalog.getWarehouseId(), getInputCatalog.getDomains().keySet()))
        .thenReturn(domainResults);

    //WHEN
    Map<InputId, Object> result = inputCatalogService.getInputsCatalog(getInputCatalog);

    //THEN
    assertEquals(2, result.size());
    assertTrue(result.containsKey(NON_SYSTEMIC_RATIO));
    assertTrue(result.containsKey(SHIFTS_PARAMETERS));
  }

  @Test
  public void invalidShiftParametersFilterTest() throws IOException {
    //GIVEN
    String expectedMessage = "Input SHIFTS_PARAMETERS only can use [INCLUDE_DAY_NAME, INCLUDE_SHIFT_GROUP] parameters";
    final Map<InputId, String> domainResults = Map.of(
        SHIFTS_PARAMETERS, getResourceAsString("inputcatalog/response_with_filters/shift_parameters.json")
    );

    final GetInputCatalog getInputCatalog = new GetInputCatalog(WAREHOUSE_ID, Map.of(
        SHIFTS_PARAMETERS, Map.of(
            INCLUDE_PROCESS.toJson(), List.of("fbm_wms_inbound"),
            INCLUDE_STAGE.toJson(), List.of("put_away")
        )
    ));

    when(inputCatalogRepository.getInputs(getInputCatalog.getWarehouseId(), getInputCatalog.getDomains().keySet()))
        .thenReturn(domainResults);

    //WHEN

    final InvalidInputFilterException exception = assertThrows(InvalidInputFilterException.class,
        () -> inputCatalogService.getInputsCatalog(getInputCatalog));

    //THEN
    assertEquals(expectedMessage, exception.getMessage());
  }

  @Test
  public void invalidNonSystemicRatioFilterTest() throws IOException {
    //GIVEN
    String expectedMessage = "Input NON_SYSTEMIC_RATIO only can use [INCLUDE_PROCESS, INCLUDE_STAGE] parameters";
    final Map<InputId, String> domainResults = Map.of(
        NON_SYSTEMIC_RATIO, getResourceAsString("inputcatalog/response_with_filters/non_systemic_ratio.json")
    );

    final GetInputCatalog getInputCatalog = new GetInputCatalog(WAREHOUSE_ID, Map.of(
        NON_SYSTEMIC_RATIO, Map.of(
            INCLUDE_DAY_NAME.toJson(), List.of("Mon", "Thu"),
            INCLUDE_SHIFT_GROUP.toJson(), List.of("AFTERNOON01")
        )
    ));

    when(inputCatalogRepository.getInputs(getInputCatalog.getWarehouseId(), getInputCatalog.getDomains().keySet()))
        .thenReturn(domainResults);

    //WHEN

    final InvalidInputFilterException exception = assertThrows(InvalidInputFilterException.class,
        () -> inputCatalogService.getInputsCatalog(getInputCatalog));

    //THEN
    assertEquals(expectedMessage, exception.getMessage());
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
                INCLUDE_STAGE.toJson(), List.of("put_away")
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
