package com.mercadolibre.planning.model.api.web.controller;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.api.domain.usecase.ratios.PreloadPackingWallRatiosUseCase;
import com.mercadolibre.planning.model.api.web.controller.ratios.RatiosJobController;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(controllers = RatiosJobController.class)
class RatiosJobControllerTest {

  private static final String CURRENT_DATE = "2023-02-12T14:30:00Z";

  private static final String DATE_FROM = "2023-01-25T18:30:00Z";

  private static final String DATE_TO = "2023-02-03T21:30:00Z";

  private static final String URL = "/planning/model/ratios";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private PreloadPackingWallRatiosUseCase preloadPackingWallRatiosUseCase;

  @MockBean
  private RequestClock requestClock;

  @Test
  void testRequestWithoutDates() throws Exception {
    var firstDate = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();

    when(requestClock.now()).thenReturn(firstDate);

    when(preloadPackingWallRatiosUseCase.execute(any(), any()))
        .thenReturn(Collections.emptyList());

    final ResultActions result = mvc.perform(
        post(URL + "/packing_wall/preload")
            .contentType(APPLICATION_JSON)
    );

    result.andExpect(status().isOk());

    verify(preloadPackingWallRatiosUseCase).execute(argThat(dateFrom -> {
      assertEquals(dateFrom, Instant.parse("2023-02-12T13:00:00Z"));
      return true;
    }), argThat(dateTo -> {
      assertEquals(dateTo, Instant.parse("2023-02-12T14:00:00Z"));
      return true;
    }));
  }

  @Test
  void testRequestWithDates() throws Exception {
    var firstDate = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();

    when(requestClock.now()).thenReturn(firstDate);

    when(preloadPackingWallRatiosUseCase.execute(any(), any()))
        .thenReturn(Collections.emptyList());

    final ResultActions result = mvc.perform(
        post(URL + "/packing_wall/preload")
            .contentType(APPLICATION_JSON)
            .param("date_from", DATE_FROM)
            .param("date_to", DATE_TO)
    );

    result.andExpect(status().isOk());

    verify(preloadPackingWallRatiosUseCase).execute(argThat(dateFrom -> {
      assertEquals(dateFrom, Instant.parse(DATE_FROM).truncatedTo(ChronoUnit.HOURS));
      return true;
    }), argThat(dateTo -> {
      assertEquals(dateTo, Instant.parse(DATE_TO).truncatedTo(ChronoUnit.HOURS));
      return true;
    }));
  }

  @Test
  void testWithDateFromGreaterThanDateTo() throws Exception {
    var firstDate = OffsetDateTime.parse(CURRENT_DATE, ISO_DATE_TIME).toInstant();

    when(requestClock.now()).thenReturn(firstDate);

    when(preloadPackingWallRatiosUseCase.execute(any(), any()))
        .thenReturn(Collections.emptyList());

    final ResultActions result = mvc.perform(
        post(URL + "/packing_wall/preload")
            .contentType(APPLICATION_JSON)
            .param("date_from", DATE_TO)
            .param("date_to", DATE_FROM)
    );

    result.andExpect(status().isBadRequest());
  }
}
