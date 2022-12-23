package com.mercadolibre.planning.model.api.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PathTest {
  @Test
  void testGetName() {
    assertEquals(Path.COLLECT.getName(), "collect");
    assertEquals(Path.FTL.getName(), "ftl");
    assertEquals(Path.PRIVATE.getName(), "private");
    assertEquals(Path.SPD.getName(), "spd");
    assertEquals(Path.TRANSFER_SHIPMENT.getName(), "transfer_shipment");
  }
}
