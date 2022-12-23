package com.mercadolibre.planning.model.api.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DeviationTypeTest {
  @Test
  void testGetName() {
    assertEquals(DeviationType.MINUTES.getName(), "minutes");
    assertEquals(DeviationType.UNITS.getName(), "units");
  }
}
