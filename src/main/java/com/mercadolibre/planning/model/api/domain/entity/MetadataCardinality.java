package com.mercadolibre.planning.model.api.domain.entity;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MetadataCardinality {
  WEEK("week"),
  WAREHOUSE_ID("warehouse_id"),
  VERSION("version"),
  UNITS_PER_ORDER_RATIO("units_per_order_ratio"),
  UNITS_PER_TOTE_RATIO("units_per_tote_ratio"),
  ORDERS_PER_PALLET_RATIO("orders_per_pallet_ratio"),
  OUTBOUND_WALL_IN_PRODUCTIVITY("outbound_wall_in_productivity"),
  OUTBOUND_PICKING_PRODUCTIVITY("outbound_picking_productivity"),
  OUTBOUND_PACKING_WALL_PRODUCTIVITY("outbound_packing_wall_productivity"),
  OUTBOUND_PACKING_PRODUCTIVITY("outbound_packing_productivity"),
  OUTBOUND_BATCH_SORTER_PRODUCTIVITY("outbound_batch_sorter_productivity"),
  MULTI_ORDER_DISTRIBUTION("multi_order_distribution"),
  MULTI_BATCH_DISTRIBUTION("multi_batch_distribution"),
  MONO_ORDER_DISTRIBUTION("mono_order_distribution");


  private static final Map<String, MetadataCardinality> LOOKUP =
      Arrays.stream(values()).collect(
          toMap(MetadataCardinality::toString, Function.identity())
      );
  private final String tagName;

  public static Optional<MetadataCardinality> of(final String value) {
    return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.ROOT)));
  }

  public static List<String> getMetadataTag() {
    return Arrays.stream(MetadataCardinality.values())
        .map(MetadataCardinality::getTagName)
        .collect(Collectors.toList());
  }

  @JsonValue
  public String toJson() {
    return this.toString().toLowerCase(Locale.ROOT);
  }
}
