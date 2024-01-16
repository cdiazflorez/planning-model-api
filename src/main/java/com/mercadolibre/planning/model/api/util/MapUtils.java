package com.mercadolibre.planning.model.api.util;

import java.util.HashMap;
import java.util.Map;

public final class MapUtils {

  // Hashmap default load factor: determines when to resize the hashmap (capacity * load factor = resize threshold)
  public static final double DEFAULT_LOAD_FACTOR = 0.75;

  private MapUtils() {

  }

  public static <K, V> Map<K, V> mapWithSize(final int size) {
    return new HashMap<>((int) Math.ceil(size / DEFAULT_LOAD_FACTOR));
  }
}
