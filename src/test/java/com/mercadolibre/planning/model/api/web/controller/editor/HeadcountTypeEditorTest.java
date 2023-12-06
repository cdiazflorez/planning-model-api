package com.mercadolibre.planning.model.api.web.controller.editor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HeadcountTypeEditorTest {

  private final HeadcountTypeEditor editor = new HeadcountTypeEditor();

  @Test
  @DisplayName("setAsText should work")
  void testGetOk() {
    // WHEN
    editor.setAsText(StaffingPlanRequest.HeadcountType.SYSTEMIC.toJson());

    // THEN
    assertAll(
        () -> assertEquals(StaffingPlanRequest.HeadcountType.class, editor.getValue().getClass()),
        () -> assertEquals(StaffingPlanRequest.HeadcountType.SYSTEMIC, editor.getValue())
    );
  }

  @Test
  @DisplayName("setAsText should failed because")
    void testTextIsBlank() {
        // WHEN
        final IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> editor.setAsText("invalid_headcount_type")
        );
        assertEquals(
            "Value invalid_headcount_type should be one of [SYSTEMIC, NON_SYSTEMIC]",
            exception.getMessage()
        );
    }

}
