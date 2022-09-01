package com.mercadolibre.planning.model.api.domain.entity;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductivityPolyvalenceCardinality {

  RECEIVING_POLYVALENCE("inbound_receiving_productivity_polyvalences"),
  PUT_AWAY_POLYVALENCE("inbound_putaway_productivity_polivalences"),
  CHECK_IN_POLYVALENCE("inbound_checkin_productivity_polyvalences"),
  WALL_IN_POLYVALENCE("outbound_wall_in_productivity"),
  PICKING_POLYVALENCE("outbound_picking_productivity"),
  PACKING_WALL_POLYVALENCE("outbound_packing_wall_productivity"),
  PACKING_POLYVALENCE("outbound_packing_productivity"),
  BATCH_SORTER_POLYVALENCE("outbound_batch_sorter_productivity");


  private static final Map<String, ProductivityPolyvalenceCardinality> LOOKUP =
      Arrays.stream(values()).collect(
          toMap(ProductivityPolyvalenceCardinality::toString, Function.identity())
      );
  private final String tagName;

  public static Optional<ProductivityPolyvalenceCardinality> of(final String value) {
    return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.ROOT)));
  }

  @JsonValue
  public String toJson() {
    return this.toString().toLowerCase(Locale.ROOT);
  }

}
