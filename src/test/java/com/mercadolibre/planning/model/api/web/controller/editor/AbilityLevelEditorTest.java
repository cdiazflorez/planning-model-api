package com.mercadolibre.planning.model.api.web.controller.editor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AbilityLevelEditorTest {

  private final AbilityLevelEditor editor = new AbilityLevelEditor();

  @Test
  @DisplayName("setAsText should work")
  void testGetOk() {
    // WHEN
    editor.setAsText(StaffingPlanRequest.AbilityLevel.MAIN.name());

    // THEN
    assertAll(
        () -> assertEquals(StaffingPlanRequest.AbilityLevel.class, editor.getValue().getClass()),
        () -> assertEquals(StaffingPlanRequest.AbilityLevel.MAIN, editor.getValue())
    );
  }

  @Test
  @DisplayName("setAsText should failed because")
  void testTextIsBlank() {
    // WHEN
    final IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> editor.setAsText("invalid_Ability_level")
    );
    assertEquals(
        "Value invalid_Ability_level should be one of [MAIN, POLYVALENT]",
        exception.getMessage()
    );
  }
}

