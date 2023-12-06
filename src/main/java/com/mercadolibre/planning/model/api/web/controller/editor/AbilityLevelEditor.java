package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import java.beans.PropertyEditorSupport;
import java.util.Arrays;

public class AbilityLevelEditor extends PropertyEditorSupport {
  @Override
  public void setAsText(final String text) {
    if (text.isBlank()) {
      throw new IllegalArgumentException("Value should not be blank");
    }
    final StaffingPlanRequest.AbilityLevel abilityLevel = StaffingPlanRequest.AbilityLevel.of(text).orElseThrow(() -> {
      final String message = String.format("Value %s should be one of %s",
          text,
          Arrays.toString(StaffingPlanRequest.AbilityLevel.values()));
      return new IllegalArgumentException(message);
    });

    setValue(abilityLevel);
  }

}
