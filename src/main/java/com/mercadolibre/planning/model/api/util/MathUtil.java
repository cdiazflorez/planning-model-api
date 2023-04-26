package com.mercadolibre.planning.model.api.util;

public final class MathUtil {

  private MathUtil() {}

  public static double safeDiv(final double numerator, final double denominator) {
    return denominator == 0D ? 0D : numerator / denominator;
  }

  public static float safeDiv(final float numerator, final float denominator) {
    return denominator == 0F ? 0F : numerator / denominator;
  }
}
