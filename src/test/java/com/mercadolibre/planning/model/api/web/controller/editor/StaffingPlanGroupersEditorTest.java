package com.mercadolibre.planning.model.api.web.controller.editor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StaffingPlanGroupersEditorTest {

  private final StaffingPlanGroupersEditor editor = new StaffingPlanGroupersEditor();

  @Test
  @DisplayName("setAsText should work")
  void testGetOk() {
    // WHEN
    editor.setAsText(StaffingPlanRequest.Groupers.DATE.name());

    // THEN
    assertAll(
        () -> assertEquals(StaffingPlanRequest.Groupers.class, editor.getValue().getClass()),
        () -> assertEquals(StaffingPlanRequest.Groupers.DATE, editor.getValue())
    );
  }

  @Test
  @DisplayName("setAsText should failed because")
  void testTextIsBlank() {
    // WHEN
    final IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> editor.setAsText("invalid_grouper")
    );
    assertEquals(
        "Value invalid_grouper should be one of [PROCESS_NAME, PROCESS_PATH, HEADCOUNT_TYPE, ABILITY_LEVEL, DATE]",
        exception.getMessage()
    );
  }
}
