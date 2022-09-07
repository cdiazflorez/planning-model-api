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
public enum ProductivityPolyvalenceCardinality {

  RECEIVING_POLYVALENCE("inbound_receiving_productivity_polyvalences", Workflow.FBM_WMS_INBOUND),
  PUT_AWAY_POLYVALENCE("inbound_putaway_productivity_polivalences", Workflow.FBM_WMS_INBOUND),
  CHECK_IN_POLYVALENCE("inbound_checkin_productivity_polyvalences", Workflow.FBM_WMS_INBOUND),
  WALL_IN_POLYVALENCE("outbound_wall_in_productivity", Workflow.FBM_WMS_OUTBOUND),
  PICKING_POLYVALENCE("outbound_picking_productivity", Workflow.FBM_WMS_OUTBOUND),
  PACKING_WALL_POLYVALENCE("outbound_packing_wall_productivity", Workflow.FBM_WMS_OUTBOUND),
  PACKING_POLYVALENCE("outbound_packing_productivity", Workflow.FBM_WMS_OUTBOUND),
  BATCH_SORTER_POLYVALENCE("outbound_batch_sorter_productivity", Workflow.FBM_WMS_OUTBOUND);


  private static final Map<String, ProductivityPolyvalenceCardinality> LOOKUP =
      Arrays.stream(values()).collect(
          toMap(ProductivityPolyvalenceCardinality::toString, Function.identity())
      );
  private final String tagName;

  private final Workflow workflow;

  public static Optional<ProductivityPolyvalenceCardinality> of(final String value) {
    return Optional.ofNullable(LOOKUP.get(value.toUpperCase(Locale.ROOT)));
  }

  public static List<ProductivityPolyvalenceCardinality> getPolyvalencesByWorkflow(final Workflow workflow) {
    return Arrays.stream(ProductivityPolyvalenceCardinality.values())
        .filter(polyvalence -> polyvalence.getWorkflow().equals(workflow))
        .collect(Collectors.toList());

  }

  @JsonValue
  public String toJson() {
    return this.toString().toLowerCase(Locale.ROOT);
  }

}
