package com.mercadolibre.planning.model.api.web.controller.plan.staffing.request;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS_NS;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.HeadcountType.NON_SYSTEMIC;
import static com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.StaffingPlanRequest.HeadcountType.SYSTEMIC;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    List<AbilityLevel> abilityLevels,
    List<StaffingPlanRequest.HeadcountType> headcountTypes,
    List<ProcessName> processes
) {
  public List<ProcessPath> getProcessPathAsEnum() {
    return processPaths() == null || processPaths().isEmpty()
        ? null
        : processPaths().stream().map(ProcessPath::of).filter(Optional::isPresent).map(Optional::get).toList();
  }

  @Getter
  @AllArgsConstructor
  public enum HeadcountType {
    SYSTEMIC(EFFECTIVE_WORKERS),
    NON_SYSTEMIC(EFFECTIVE_WORKERS_NS);

    private static final Map<String, HeadcountType> LOOKUP = Arrays.stream(values()).collect(
        toMap(HeadcountType::toString, Function.identity())
    );

    private final ProcessingType value;

    @JsonCreator
    public static Optional<HeadcountType> of(final String value) {
      return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US)));
    }

    @JsonValue
    public String toJson() {
      return toString().toLowerCase(Locale.US);
    }
  }

  @Getter
  @AllArgsConstructor
  public enum AbilityLevel {
    MAIN(1),
    POLYVALENT(2);

    private static final Map<String, AbilityLevel> LOOKUP = Arrays.stream(values()).collect(
        toMap(AbilityLevel::toString, Function.identity())
    );

    private final int value;

    @JsonCreator
    public static Optional<AbilityLevel> of(final String value) {
      return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US)));
    }
  }

  @Getter
  @AllArgsConstructor
  public enum Groupers {
    PROCESS_NAME(Groupers::getProcessName),
    PROCESS_PATH(Groupers::getProcessPath),
    HEADCOUNT_TYPE(Groupers::getHeadcountType),
    ABILITY_LEVEL(Groupers::getAbilityLevel),
    DATE(Groupers::getDate);

    private static final Map<String, Groupers> LOOKUP = Arrays.stream(values()).collect(
        toMap(Groupers::toString, Function.identity())
    );

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
      final var headcountType = eo.getType() == EFFECTIVE_WORKERS ? SYSTEMIC : NON_SYSTEMIC;
      return headcountType.toJson();
    }

    @JsonCreator
    public static Optional<Groupers> of(final String value) {
      return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.US)));
    }

    @JsonValue
    public String toJson() {
      return toString().toLowerCase(Locale.US);
    }
  }
}
