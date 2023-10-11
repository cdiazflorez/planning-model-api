package com.mercadolibre.planning.model.api.domain.service.lastupdatedentity;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
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
class LastEntityModifiedDateServiceTest {

  private static final Set<EntityType> ENTITY_TYPES = Set.of(
      EntityType.HEADCOUNT_SYSTEMIC,
      EntityType.HEADCOUNT_NON_SYSTEMIC,
      EntityType.PRODUCTIVITY
  );

  private static final Instant VIEW_DATE = A_DATE_UTC.toInstant();

  @Mock
  private LastEntityModifiedDateService.GetLastModifiedDateGateway getLastModifiedDateGateway;

  @InjectMocks
  private LastEntityModifiedDateService lastEntityModifiedDateService;

  public static Stream<Arguments> parametersCaseTest() {
    return Stream.of(
        Arguments.of(ENTITY_TYPES),
        Arguments.of(Collections.emptySet())
    );
  }

  @ParameterizedTest
  @MethodSource("parametersCaseTest")
  void testGetLastEntityDateModifiedOk(final Set<EntityType> entityTypes) {


    when(getLastModifiedDateGateway.getLastDateStaffingCreated(WAREHOUSE_ID, Workflow.FBM_WMS_OUTBOUND, VIEW_DATE))
        .thenReturn(VIEW_DATE.minus(15, ChronoUnit.HOURS));


    if (entityTypes != null && !entityTypes.isEmpty()) {
      when(getLastModifiedDateGateway.getLastDateEntitiesCreated(
          WAREHOUSE_ID,
          Workflow.FBM_WMS_OUTBOUND,
          Set.of(EntityType.HEADCOUNT_SYSTEMIC, EntityType.HEADCOUNT_NON_SYSTEMIC, EntityType.PRODUCTIVITY),
          VIEW_DATE.minus(15, ChronoUnit.HOURS)
      )).thenReturn(Map.of(
          EntityType.HEADCOUNT_SYSTEMIC, VIEW_DATE.minus(10, ChronoUnit.HOURS),
          EntityType.HEADCOUNT_NON_SYSTEMIC, VIEW_DATE.minus(7, ChronoUnit.HOURS),
          EntityType.PRODUCTIVITY, VIEW_DATE.minus(13, ChronoUnit.HOURS)
      ));
    }


    final LastModifiedDates responseService = lastEntityModifiedDateService.getLastEntityDateModified(
        WAREHOUSE_ID,
        Workflow.FBM_WMS_OUTBOUND,
        entityTypes,
        VIEW_DATE
    );
    if (entityTypes == null || entityTypes.isEmpty()) {
      Assertions.assertEquals(VIEW_DATE.minus(15, ChronoUnit.HOURS), responseService.lastStaffingCreated());
      Assertions.assertTrue(responseService.lastDateEntitiesUpdate().isEmpty());
    } else {
      Assertions.assertEquals(VIEW_DATE.minus(15, ChronoUnit.HOURS), responseService.lastStaffingCreated());
      Assertions.assertEquals(3, responseService.lastDateEntitiesUpdate().size());
      Assertions.assertTrue(responseService.lastDateEntitiesUpdate().containsKey(EntityType.HEADCOUNT_SYSTEMIC));
      Assertions.assertTrue(responseService.lastDateEntitiesUpdate().containsKey(EntityType.PRODUCTIVITY));
      Assertions.assertTrue(responseService.lastDateEntitiesUpdate().containsKey(EntityType.HEADCOUNT_NON_SYSTEMIC));
    }


  }


}
