package com.mercadolibre.planning.model.api.web.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestCLockTest {

  @Test
  void testRequestClockNow() {

    try (MockedStatic<Instant> mocked = mockStatic(Instant.class)) {
      // Create a mock for the static method to return
      var mockInstant = mock(Instant.class);
      when(mockInstant.getEpochSecond()).thenReturn(1L);

      // Stub the static method .now()
      mocked.when(Instant::now).thenReturn(mockInstant);
      final var clock = new RequestClock();
      var result = clock.now();

      assertThat(result.getEpochSecond()).isEqualTo(1);
    }
  }
}
