package com.mercadolibre.planning.model.api.adapter.staffing;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockMaxDateEditionByType;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingMaxDateCreatedByType;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetLastModifiedDateAdapterTest {

  private static final List<Long> FORECAST_IDS = List.of(122L, 125L, 124L);

  private static final Set<ProcessingType> PROCESSING_TYPES = Set.of(
      ProcessingType.EFFECTIVE_WORKERS,
      ProcessingType.EFFECTIVE_WORKERS_NS,
      ProcessingType.PRODUCTIVITY
  );

  private static final Set<EntityType> ENTITY_TYPES = Set.of(
      EntityType.HEADCOUNT_SYSTEMIC,
      EntityType.HEADCOUNT_NON_SYSTEMIC,
      EntityType.PRODUCTIVITY
  );

  private static final ZonedDateTime DATE = ZonedDateTime.of(2023, 9, 28, 0, 0, 0, 0, ZoneOffset.UTC);

  @Mock
  private GetForecastUseCase getForecastUseCase;
  @Mock
  private ForecastRepository repository;
  @Mock
  private CurrentProcessingDistributionRepository currentProcessingDistributionRepository;

  @InjectMocks
  private GetLastModifiedDateAdapter getLastModifiedDateAdapter;

  public static Stream<Arguments> lastStaffingDateCreatedArguments() {
    return Stream.of(
        Arguments.of(FORECAST_IDS),
        Arguments.of(Collections.emptyList())
    );
  }

  public static Stream<Arguments> lastEntitiesDateCreated() {
    return Stream.of(
        Arguments.of(
            mockMaxDateEditionByType(),
            Map.of(
                EntityType.HEADCOUNT_SYSTEMIC, DATE.plus(22, ChronoUnit.HOURS).toInstant(),
                EntityType.HEADCOUNT_NON_SYSTEMIC, DATE.plus(21, ChronoUnit.HOURS).toInstant(),
                EntityType.PRODUCTIVITY, DATE.plus(23, ChronoUnit.HOURS).toInstant()
            )),
        Arguments.of(
            Collections.emptyList(),
            Collections.emptyMap())
    );
  }


  @ParameterizedTest
  @MethodSource("lastStaffingDateCreatedArguments")
  void testGetLastDateStaffingCreated(final List<Long> forecastIds) {
    when(getForecastUseCase.execute(any())).thenReturn(forecastIds);

    if (!forecastIds.isEmpty()) {
      when(repository.findById(FORECAST_IDS.get(1))).thenReturn(Optional.of(mockSimpleForecast()));
    }

    final Instant lastStaffingPlanCreated = getLastModifiedDateAdapter
        .getLastDateStaffingCreated(WAREHOUSE_ID, Workflow.FBM_WMS_OUTBOUND, A_DATE_UTC.plus(1, ChronoUnit.DAYS).toInstant());


    if (!forecastIds.isEmpty()) {
      Assertions.assertEquals(lastStaffingPlanCreated, A_DATE_UTC.toInstant());
    } else {
      Assertions.assertNull(lastStaffingPlanCreated);
    }

  }

  @ParameterizedTest
  @MethodSource("lastEntitiesDateCreated")
  void testGetLastDateEntitiesCreated(final List<CurrentProcessingMaxDateCreatedByType> maxDateCreatedByType,
                                      final Map<EntityType, Instant> lastDateEntitiesCreatedExpected) {
    // WHEN
    when(currentProcessingDistributionRepository.findDateCreatedByWarehouseIdAndWorkflowAndTypeAndIsActive(
        WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        PROCESSING_TYPES,
        DATE
    )).thenReturn(maxDateCreatedByType);

    final Map<EntityType, Instant> lastDateEntitiesCreated = getLastModifiedDateAdapter.getLastDateEntitiesCreated(
        WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        ENTITY_TYPES,
        DATE.toInstant()
    );

    // THEN
    if (maxDateCreatedByType.isEmpty()) {

      Assertions.assertEquals(lastDateEntitiesCreatedExpected, lastDateEntitiesCreated);

    } else {

      Assertions.assertTrue(
          lastDateEntitiesCreated.entrySet()
              .stream().filter(entityType -> entityType.getKey() == EntityType.HEADCOUNT_NON_SYSTEMIC)
              .anyMatch(entity -> lastDateEntitiesCreatedExpected.get(EntityType.HEADCOUNT_NON_SYSTEMIC).equals(entity.getValue()))
      );

      Assertions.assertTrue(
          lastDateEntitiesCreated.entrySet()
              .stream().filter(entityType -> entityType.getKey() == EntityType.HEADCOUNT_SYSTEMIC)
              .anyMatch(entity -> lastDateEntitiesCreatedExpected.get(EntityType.HEADCOUNT_SYSTEMIC).equals(entity.getValue()))
      );

      Assertions.assertTrue(
          lastDateEntitiesCreated.entrySet()
              .stream().filter(entityType -> entityType.getKey() == EntityType.PRODUCTIVITY)
              .anyMatch(entity -> lastDateEntitiesCreatedExpected.get(EntityType.PRODUCTIVITY).equals(entity.getValue()))
      );
    }


  }
}
