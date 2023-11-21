package com.mercadolibre.planning.model.api.web.controller.plan.staffing.request;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;

import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

public record StaffingPlanRequest(
    List<EntityType> resources,
    Workflow workflow,
    ZonedDateTime dateFrom,
    ZonedDateTime dateTo,
    Instant viewDate,
    String warehouseId,
    List<Groupers> groupers,
    List<String> processPaths,
    List<ProcessName> processes
) {
  public List<ProcessPath> getProcessPathAsEnum() {
    return processPaths() == null || processPaths().isEmpty()
        ? null
        : processPaths().stream().map(ProcessPath::of).filter(Optional::isPresent).map(Optional::get).toList();
  }

  @Getter
  @AllArgsConstructor
  public enum Groupers {
    PROCESS_NAME(Groupers::getProcessName),
    PROCESS_PATH(Groupers::getProcessPath),
    HEADCOUNT_TYPE(Groupers::getHeadcountType),
    ABILITY_LEVEL(Groupers::getAbilityLevel),
    DATE(Groupers::getDate);

    private final Function<EntityOutput, String> valueGetter;

    private static String getAbilityLevel(EntityOutput eo) {
      if (eo instanceof ProductivityOutput productivityOutput) {
        return String.valueOf(productivityOutput.getAbilityLevel());
      }
      return "NAN";
    }

    private static String getDate(EntityOutput eo) {
      return eo.getDate().toInstant().toString();
    }

    private static String getProcessName(EntityOutput eo) {
      return eo.getProcessName().toJson();
    }

    private static String getProcessPath(EntityOutput eo) {
      return eo.getProcessPath().toJson();
    }

    private static String getHeadcountType(EntityOutput eo) {
      return eo.getType() == EFFECTIVE_WORKERS ? "systemic" : "non_systemic";
    }

    @JsonValue
    public String toJson() {
      return toString().toLowerCase(Locale.US);
    }
  }
}
