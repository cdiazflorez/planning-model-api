package com.mercadolibre.planning.model.api.client.db.repository.current;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.exception.TagsParsingException;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrentProcessingDistributionImplTest {
  private static final String LOGISTIC_CENTER_ID = "ARTW01";
  private static final String PROCESS_NAME = "PICKING";
  private static final String PROCESS_NAME_KEY = "process";
  private static final String PROCESS_PATH_KEY = "process_path";

  @Mock
  private CurrentProcessingDistributionRepository currentProcessingDistributionRepository;

  @Mock
  private ObjectMapper objectMapper;

  private CurrentProcessingDistributionImpl currentProcessingDistributionImpl;

  @BeforeEach
  void setUp() {
    currentProcessingDistributionImpl = new CurrentProcessingDistributionImpl(
        currentProcessingDistributionRepository,
        objectMapper
    );
    when(objectMapper.getSerializationConfig()).thenReturn(mock(SerializationConfig.class));
  }

  @Test
  void throwErrorParsingTags() throws JsonProcessingException {
    // given
    final Workflow workflow = Workflow.FBM_WMS_OUTBOUND;
    final String logisticCenterId = LOGISTIC_CENTER_ID;
    final ZonedDateTime date = ZonedDateTime.now();
    final Map<String, String> tags = Map.of(PROCESS_NAME_KEY, PROCESS_NAME);
    final long userId = 1L;
    final EntityType type = EntityType.HEADCOUNT;

    // when
    when(objectMapper.writeValueAsString(tags)).thenThrow(JsonProcessingException.class);

    // then
    assertThrows(TagsParsingException.class, () ->
        currentProcessingDistributionImpl.deactivateStaffingUpdate(
            workflow,
            logisticCenterId,
            date,
            tags,
            userId,
            type.getProcessingType(),
            type.getMetricUnit()
        )
    );
  }

  @Test
  void deactivateStaffingUpdate() throws JsonProcessingException {
    // given
    final Workflow workflow = Workflow.FBM_WMS_OUTBOUND;
    final String logisticCenterId = LOGISTIC_CENTER_ID;
    final ZonedDateTime date = ZonedDateTime.now();
    final Map<String, String> tags = Map.of(PROCESS_NAME_KEY, PROCESS_NAME);
    final long userId = 1L;
    final EntityType type = EntityType.HEADCOUNT;

    // when
    currentProcessingDistributionImpl.deactivateStaffingUpdate(
        workflow,
        logisticCenterId,
        date,
        tags,
        userId,
        type.getProcessingType(),
        type.getMetricUnit()
    );

    // then
    verify(currentProcessingDistributionRepository).deactivateProcessingDistributionByTags(
        workflow,
        date,
        logisticCenterId,
        objectMapper.writeValueAsString(tags),
        type.getProcessingType(),
        type.getMetricUnit(),
        userId
    );
  }

  @Test
  void throwErrorParsingTagsCreateStaffingUpdates() throws JsonProcessingException {
    // given
    final var tags = Map.of(PROCESS_NAME_KEY, PROCESS_NAME);

    // when
    when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

    // then
    assertThrows(TagsParsingException.class, () ->
        currentProcessingDistributionImpl.createStaffingUpdates(
            List.of(
                new UpdateStaffingPlanUseCase.CreateSimulationInput(
                    Workflow.FBM_WMS_OUTBOUND,
                    LOGISTIC_CENTER_ID,
                    1L,
                    tags,
                    1.0,
                    ZonedDateTime.now(),
                    EntityType.HEADCOUNT
                )
            )
        )
    );
  }

  @Test
  void createStaffingUpdates() {
    // given
    final var inputs = List.of(
        createSimulationInput(10, DATE_IN, EntityType.HEADCOUNT, Map.of(PROCESS_NAME_KEY, PROCESS_NAME)),
        createSimulationInput(11, DATE_IN.plusHours(1), EntityType.HEADCOUNT, Map.of()),
        createSimulationInput(12, DATE_IN.plusHours(2), EntityType.HEADCOUNT, Map.of()),
        createSimulationInput(10, DATE_IN, EntityType.PRODUCTIVITY, Map.of(PROCESS_PATH_KEY, "tot_mono")),
        createSimulationInput(10, DATE_IN, EntityType.MAX_CAPACITY, null)
    );

    final var results = List.of(
        createCurrentProcessingDistribution(
            10, DATE_IN, EntityType.HEADCOUNT, ProcessName.PICKING, GLOBAL
        ),
        createCurrentProcessingDistribution(
            11, DATE_IN.plusHours(1), EntityType.HEADCOUNT, ProcessName.GLOBAL, GLOBAL
        ),
        createCurrentProcessingDistribution(
            12, DATE_IN.plusHours(2), EntityType.HEADCOUNT, ProcessName.GLOBAL, GLOBAL
        ),
        createCurrentProcessingDistribution(
            10, DATE_IN, EntityType.PRODUCTIVITY, ProcessName.GLOBAL, TOT_MONO
        ),
        createCurrentProcessingDistribution(
            10, DATE_IN, EntityType.MAX_CAPACITY, ProcessName.GLOBAL, GLOBAL
        )
    );

    // when
    currentProcessingDistributionImpl.createStaffingUpdates(inputs);

    // then
    verify(currentProcessingDistributionRepository).saveAll(results);
  }

  private UpdateStaffingPlanUseCase.CreateSimulationInput createSimulationInput(
      final double quantity,
      final ZonedDateTime date,
      final EntityType entityType,
      final Map<String, String> tags
  ) {
    return new UpdateStaffingPlanUseCase.CreateSimulationInput(
        Workflow.FBM_WMS_OUTBOUND,
        LOGISTIC_CENTER_ID,
        1L,
        tags,
        quantity,
        date,
        entityType
    );
  }

  private CurrentProcessingDistribution createCurrentProcessingDistribution(
      final double quantity,
      final ZonedDateTime date,
      final EntityType entityType,
      final ProcessName processName,
      final ProcessPath processPath
  ) {
    return CurrentProcessingDistribution.builder()
        .workflow(Workflow.FBM_WMS_OUTBOUND)
        .processName(processName)
        .processPath(processPath)
        .date(date)
        .quantity(quantity)
        .logisticCenterId(LOGISTIC_CENTER_ID)
        .userId(1L)
        .type(entityType.getProcessingType())
        .quantityMetricUnit(entityType.getMetricUnit())
        .tags(null)
        .isActive(true)
        .build();
  }


}
