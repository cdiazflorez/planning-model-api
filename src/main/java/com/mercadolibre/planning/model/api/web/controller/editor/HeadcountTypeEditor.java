package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest;
import java.beans.PropertyEditorSupport;
import java.util.Arrays;

public class HeadcountTypeEditor extends PropertyEditorSupport {

  @Override
  public void setAsText(final String text) {
    final StaffingPlanRequest.HeadcountType headcountType = StaffingPlanRequest.HeadcountType.of(text).orElseThrow(() -> {
      final String message = String.format("Value %s should be one of %s",
          text,
          Arrays.toString(StaffingPlanRequest.HeadcountType.values()));
      return new IllegalArgumentException(message);
    });
    setValue(headcountType);
  }
}
