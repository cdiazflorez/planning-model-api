package com.mercadolibre.planning.model.api.util;

public final class MathUtil {

  private MathUtil() {}

  public static Double safeDiv(final Double numerator, final Double denominator) {
    return denominator.equals(0D) ? 0D : numerator / denominator;
  }
}
