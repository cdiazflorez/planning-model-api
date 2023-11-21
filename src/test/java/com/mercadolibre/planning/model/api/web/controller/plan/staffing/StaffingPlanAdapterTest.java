package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.StaffingPlanTestUtils.mockHeadcount;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.StaffingPlanTestUtils.mockMaxCapacity;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.StaffingPlanTestUtils.mockProductivity;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.StaffingPlanTestUtils.mockThroughputs;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.Groupers.ABILITY_LEVEL;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.Groupers.DATE;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.Groupers.HEADCOUNT_TYPE;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.Groupers.PROCESS_NAME;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.Groupers.PROCESS_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.exception.EntityTypeNotSupportedException;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffingPlanAdapterTest {

  private static final String PROCESS_NAME_KEY = "process_name";

  private static final String PROCESS_PATH_KEY = "process_path";

  private static final String HEADCOUNT_TYPE_KEY = "headcount_type";

  private static final String ABILITY_LEVEL_KEY = "ability_level";

  private static final String DATE_KEY = "date";

  @Mock
  private GetHeadcountEntityUseCase headcountUseCase;

  @Mock
  private GetProductivityEntityUseCase productivityUseCase;

  @Mock
  private GetThroughputUseCase throughputUseCase;

  private StaffingPlanAdapter staffingPlanAdapter;

  @BeforeEach
  void setup() {
    staffingPlanAdapter = new StaffingPlanAdapter(headcountUseCase, productivityUseCase, throughputUseCase);
  }

  @Test
  void testNotAllowedEntities() {
    assertThrows(EntityTypeNotSupportedException.class, () -> staffingPlanAdapter.getStaffingPlan(
        createRequest(EntityType.BACKLOG_UPPER_LIMIT, List.of(DATE), null)
    ));
  }

  @Test
  void testGetHeadcountByProcessName() {
    when(headcountUseCase.execute(any(GetHeadcountInput.class)))
        .thenReturn(mockHeadcount());

    final var result = staffingPlanAdapter.getStaffingPlan(
        createRequest(EntityType.HEADCOUNT, List.of(DATE, PROCESS_NAME), List.of("tot_mono", "tot_multi"))
    );
    assertEquals(1, result.resources().size());
    assertEquals(EntityType.HEADCOUNT, result.resources().get(0).name());
    assertEquals(6, result.resources().get(0).values().size());
    for (final var value : result.resources().get(0).values()) {
      assertTrue(value.groupers().keySet().containsAll(List.of(DATE_KEY, PROCESS_NAME_KEY)));
    }
  }

  @Test
  void testGetHeadcountByProcessPath() {
    when(headcountUseCase.execute(any(GetHeadcountInput.class)))
        .thenReturn(mockHeadcount());

    final var result = staffingPlanAdapter.getStaffingPlan(
        createRequest(EntityType.HEADCOUNT, List.of(DATE, PROCESS_NAME, PROCESS_PATH), null)
    );
    assertEquals(1, result.resources().size());
    assertEquals(EntityType.HEADCOUNT, result.resources().get(0).name());
    assertEquals(18, result.resources().get(0).values().size());
    for (final var value : result.resources().get(0).values()) {
      assertTrue(value.groupers().keySet().containsAll(List.of(DATE_KEY, PROCESS_NAME_KEY, PROCESS_PATH_KEY)));
    }
  }

  @Test
  void testGetHeadcountByHeadcountType() {
    when(headcountUseCase.execute(any(GetHeadcountInput.class)))
        .thenReturn(mockHeadcount());

    final var result = staffingPlanAdapter.getStaffingPlan(
        createRequest(EntityType.HEADCOUNT, List.of(DATE, PROCESS_NAME, HEADCOUNT_TYPE), List.of())
    );
    assertEquals(1, result.resources().size());
    assertEquals(EntityType.HEADCOUNT, result.resources().get(0).name());
    assertEquals(6, result.resources().get(0).values().size());
    for (final var value : result.resources().get(0).values()) {
      assertTrue(value.groupers().keySet().containsAll(List.of(DATE_KEY, PROCESS_NAME_KEY, HEADCOUNT_TYPE_KEY)));
    }
  }

  @Test
  void testGetHeadcountByAbilityLevel() {
    when(headcountUseCase.execute(any(GetHeadcountInput.class)))
        .thenReturn(mockHeadcount());

    final var result = staffingPlanAdapter.getStaffingPlan(
        createRequest(EntityType.HEADCOUNT, List.of(DATE, PROCESS_NAME, ABILITY_LEVEL), List.of())
    );
    assertEquals(1, result.resources().size());
    assertEquals(EntityType.HEADCOUNT, result.resources().get(0).name());
    assertEquals(6, result.resources().get(0).values().size());
    for (final var value : result.resources().get(0).values()) {
      assertTrue(value.groupers().keySet().containsAll(List.of(DATE_KEY, PROCESS_NAME_KEY, ABILITY_LEVEL_KEY)));
      assertEquals("NAN", value.groupers().get(ABILITY_LEVEL_KEY));
    }
  }

  @Test
  void testGetProductivityByProcessName() {
    when(productivityUseCase.execute(any(GetProductivityInput.class)))
        .thenReturn(mockProductivity());

    final var result = staffingPlanAdapter.getStaffingPlan(
        createRequest(EntityType.PRODUCTIVITY, List.of(DATE, PROCESS_NAME, ABILITY_LEVEL), List.of("multi_batch"))
    );
    assertEquals(1, result.resources().size());
    assertEquals(EntityType.PRODUCTIVITY, result.resources().get(0).name());
    assertEquals(7, result.resources().get(0).values().size());
    for (final var value : result.resources().get(0).values()) {
      assertTrue(value.groupers().keySet().containsAll(List.of(DATE_KEY, PROCESS_NAME_KEY, ABILITY_LEVEL_KEY)));
    }
  }

  @Test
  void testGetProductivity() {
    when(productivityUseCase.execute(any(GetProductivityInput.class)))
        .thenReturn(mockProductivity());

    final var result = staffingPlanAdapter.getStaffingPlan(
        createRequest(EntityType.PRODUCTIVITY, List.of(DATE, PROCESS_NAME), List.of())
    );
    assertEquals(1, result.resources().size());
    assertEquals(EntityType.PRODUCTIVITY, result.resources().get(0).name());
    assertEquals(6, result.resources().get(0).values().size());
    for (final var value : result.resources().get(0).values()) {
      assertTrue(value.groupers().keySet().containsAll(List.of(DATE_KEY, PROCESS_NAME_KEY)));
    }
  }

  @Test
  void testGetMaxCapacity() {
    when(headcountUseCase.execute(any(GetHeadcountInput.class)))
        .thenReturn(mockMaxCapacity());

    final var result = staffingPlanAdapter.getStaffingPlan(
        createRequest(EntityType.MAX_CAPACITY, List.of(DATE), List.of())
    );
    assertEquals(1, result.resources().size());
    assertEquals(EntityType.MAX_CAPACITY, result.resources().get(0).name());
    assertEquals(6, result.resources().get(0).values().size());
    for (final var value : result.resources().get(0).values()) {
      assertTrue(value.groupers().containsKey(DATE_KEY));
    }
  }

  @Test
  void testGetThroughput() {
    when(throughputUseCase.execute(any(GetEntityInput.class)))
        .thenReturn(mockThroughputs());

    final var result = staffingPlanAdapter.getStaffingPlan(
        createRequest(EntityType.THROUGHPUT, List.of(DATE, PROCESS_NAME), List.of())
    );
    assertEquals(1, result.resources().size());
    assertEquals(EntityType.THROUGHPUT, result.resources().get(0).name());
    assertEquals(6, result.resources().get(0).values().size());
    for (final var value : result.resources().get(0).values()) {
      assertTrue(value.groupers().keySet().containsAll(List.of(DATE_KEY, PROCESS_NAME_KEY)));
    }
  }

  private StaffingPlanRequest createRequest(
      final EntityType entityType,
      final List<StaffingPlanRequest.Groupers> groupers,
      final List<String> processPaths
  ) {
    return new StaffingPlanRequest(
        List.of(entityType),
        Workflow.FBM_WMS_OUTBOUND,
        DATE_IN,
        DATE_OUT,
        A_DATE_UTC.toInstant(),
        "ARTW01",
        groupers,
        processPaths,
        List.of()
    );
  }
}
