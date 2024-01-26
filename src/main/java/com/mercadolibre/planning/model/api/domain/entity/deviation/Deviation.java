package com.mercadolibre.planning.model.api.domain.entity.deviation;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlannedUnitsService;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

public record Deviation(double value, Map<String, String> deviationMap) {

  private static final String DATE_IN = "date_in";

  private static final String DATE_IN_FROM = "date_in_from";

  private static final String DATE_IN_TO = "date_in_to";

  private static final String DATE_OUT = "date_out";

  private static final String DATE_OUT_FROM = "date_out_from";

  private static final String DATE_OUT_TO = "date_out_to";

  private static final Map<String, String> DEVIATION_KEY = Map.of(
      DATE_IN_FROM, DATE_IN,
      DATE_OUT_FROM, DATE_OUT,
      DATE_IN_TO, DATE_IN,
      DATE_OUT_TO, DATE_OUT
  );

  private static final Map<String, BiPredicate<String, String>> COMPARATORS = Map.of(
      DATE_IN_FROM, Deviation::lessThanOrEquals,
      DATE_IN_TO, Deviation::greaterThanOrEquals,
      DATE_OUT_FROM, Deviation::lessThanOrEquals,
      DATE_OUT_TO, Deviation::greaterThanOrEquals
  );

  private static boolean lessThanOrEquals(final String firstValue, final String secondValue) {
    return secondValue != null && firstValue.compareTo(secondValue) <= 0;
  }

  private static boolean greaterThanOrEquals(final String firstValue, final String secondValue) {
    return secondValue != null && firstValue.compareTo(secondValue) >= 0;
  }

  /**
   * Applies the deviation to the given tagged unit.
   * If the deviation criteria do not match the tagged unit properties,
   * this function will return the original tagged unit,
   * otherwise, it will return a new tagged unit with a modified quantity.
   *
   * @param taggedUnit the tagged unit to which the deviation is to be applied
   * @return The tagged unit after applying the deviation
   */
  public PlannedUnitsService.TaggedUnit applyDeviation(final PlannedUnitsService.TaggedUnit taggedUnit) {

    if (doesApplyDeviation(taggedUnit)) {
      return new PlannedUnitsService.TaggedUnit(taggedUnit.quantity() * (1 + value), taggedUnit.tags());
    }
    return taggedUnit;
  }

  /**
   * Determines whether the deviation applies to the given tagged unit.
   * This is primarily determined by comparing the key-value pairs
   * of the deviation map with the appropriate field of the tagged unit.
   *
   * @param taggedUnit the tagged unit to check
   * @return true if the deviation applies to the tagged unit, false otherwise
   */
  private boolean doesApplyDeviation(final PlannedUnitsService.TaggedUnit taggedUnit) {
    final Set<String> keys = deviationMap.keySet();

    for (String key : keys) {
      String tagKey = DEVIATION_KEY.getOrDefault(key, key);

      final String tagValue = taggedUnit.tags().get(tagKey);

      final String deviationValue = deviationMap.get(key);

      if (!COMPARATORS.getOrDefault(key, String::equals).test(deviationValue, tagValue)) {
        return false;
      }

    }
    return true;
  }

}
