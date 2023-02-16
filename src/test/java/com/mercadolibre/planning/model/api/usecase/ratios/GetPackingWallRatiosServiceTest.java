package com.mercadolibre.planning.model.api.usecase.ratios;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.ratios.RatiosRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.ratios.Ratio;
import com.mercadolibre.planning.model.api.domain.usecase.ratios.GetPackingWallRatiosService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;

@ExtendWith(MockitoExtension.class)
class GetPackingWallRatiosServiceTest {

  private static final String PW_RATIOS_TYPE = "packing_wall_ratios";

  private static final int DEFAULT_WEEKS = 3;

  private static final int DAYS_IN_A_WEEK = 7;

  private static final Instant DATE_FROM = Instant.parse("2023-01-25T16:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2023-01-25T20:00:00Z");

  private static final String ARTW01 = "ARTW01";

  private static final double DELTA = 1e-3;

  @InjectMocks
  private GetPackingWallRatiosService getPackingWallRatiosUseCase;

  @Mock
  private RatiosRepository ratiosRepository;


  @Test
  void testGetPackingWallRatiosWithAllMirrors() {
    mockRatiosWhitAllMirrors();
    final var result = getPackingWallRatiosUseCase.execute(ARTW01, DATE_FROM, DATE_TO);
    assertEquals(5, result.size());
    final var firstHour = result.get(DATE_FROM);
    assertThat(0.604, closeTo(firstHour.getPackingWallRatio(), DELTA));
    assertThat(0.395, closeTo(firstHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(firstHour.getPackingToteRatio() + firstHour.getPackingWallRatio(), DELTA));

    final var secondHour = result.get(DATE_FROM.plus(1, HOURS));
    assertThat(0.391, closeTo(secondHour.getPackingWallRatio(), DELTA));
    assertThat(0.608, closeTo(secondHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(secondHour.getPackingToteRatio() + secondHour.getPackingWallRatio(), DELTA));

    final var thirdHour = result.get(DATE_FROM.plus(2, HOURS));
    assertThat(0.220, closeTo(thirdHour.getPackingWallRatio(), DELTA));
    assertThat(0.779, closeTo(thirdHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(thirdHour.getPackingToteRatio() + thirdHour.getPackingWallRatio(), DELTA));

    final var fourthHour = result.get(DATE_FROM.plus(3, HOURS));
    assertThat(0.587, closeTo(fourthHour.getPackingWallRatio(), DELTA));
    assertThat(0.412, closeTo(fourthHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(fourthHour.getPackingToteRatio() + fourthHour.getPackingWallRatio(), DELTA));

    final var fifthHour = result.get(DATE_FROM.plus(4, HOURS));
    assertThat(0.843, closeTo(fifthHour.getPackingWallRatio(), DELTA));
    assertThat(0.156, closeTo(fifthHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(fifthHour.getPackingToteRatio() + fifthHour.getPackingWallRatio(), DELTA));
  }

  @Test
  void testGetPackingWallRatiosWithOnlyOneMirror() {
    mockRatiosWithOnlyOneMirror();
    final var result = getPackingWallRatiosUseCase.execute(ARTW01, DATE_FROM, DATE_TO);
    assertEquals(5, result.size());
    final var firstHour = result.get(DATE_FROM);
    assertThat(0.625, closeTo(firstHour.getPackingWallRatio(), DELTA));
    assertThat(0.375, closeTo(firstHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(firstHour.getPackingToteRatio() + firstHour.getPackingWallRatio(), DELTA));

    final var secondHour = result.get(DATE_FROM.plus(1, HOURS));
    assertThat(0.800, closeTo(secondHour.getPackingWallRatio(), DELTA));
    assertThat(0.200, closeTo(secondHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(secondHour.getPackingToteRatio() + secondHour.getPackingWallRatio(), DELTA));

    final var thirdHour = result.get(DATE_FROM.plus(2, HOURS));
    assertThat(0.200, closeTo(thirdHour.getPackingWallRatio(), DELTA));
    assertThat(0.800, closeTo(thirdHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(thirdHour.getPackingToteRatio() + thirdHour.getPackingWallRatio(), DELTA));

    final var fourthHour = result.get(DATE_FROM.plus(3, HOURS));
    assertThat(0.100, closeTo(fourthHour.getPackingWallRatio(), DELTA));
    assertThat(0.900, closeTo(fourthHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(fourthHour.getPackingToteRatio() + fourthHour.getPackingWallRatio(), DELTA));

    final var fifthHour = result.get(DATE_FROM.plus(4, HOURS));
    assertThat(0.325, closeTo(fifthHour.getPackingWallRatio(), DELTA));
    assertThat(0.675, closeTo(fifthHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(fifthHour.getPackingToteRatio() + fifthHour.getPackingWallRatio(), DELTA));
  }

  @Test
  void testGetPackingWallRatiosWithEmptyRatios() {
    when(ratiosRepository.findRatiosByLogisticCenterIdAndDateBetweenAndType(anyString(), any(), any(), anyString()))
        .thenThrow(BadSqlGrammarException.class);
    final var result = getPackingWallRatiosUseCase.execute(ARTW01, DATE_FROM, DATE_TO);
    assertEquals(5, result.size());
    final var firstHour = result.get(DATE_FROM);
    assertThat(0.500, closeTo(firstHour.getPackingWallRatio(), DELTA));
    assertThat(0.500, closeTo(firstHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(firstHour.getPackingToteRatio() + firstHour.getPackingWallRatio(), DELTA));

    final var secondHour = result.get(DATE_FROM.plus(1, HOURS));
    assertThat(0.500, closeTo(firstHour.getPackingWallRatio(), DELTA));
    assertThat(0.500, closeTo(firstHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(secondHour.getPackingToteRatio() + secondHour.getPackingWallRatio(), DELTA));

    final var thirdHour = result.get(DATE_FROM.plus(2, HOURS));
    assertThat(0.500, closeTo(firstHour.getPackingWallRatio(), DELTA));
    assertThat(0.500, closeTo(firstHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(thirdHour.getPackingToteRatio() + thirdHour.getPackingWallRatio(), DELTA));

    final var fourthHour = result.get(DATE_FROM.plus(3, HOURS));
    assertThat(0.500, closeTo(firstHour.getPackingWallRatio(), DELTA));
    assertThat(0.500, closeTo(firstHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(fourthHour.getPackingToteRatio() + fourthHour.getPackingWallRatio(), DELTA));

    final var fifthHour = result.get(DATE_FROM.plus(4, HOURS));
    assertThat(0.500, closeTo(firstHour.getPackingWallRatio(), DELTA));
    assertThat(0.500, closeTo(firstHour.getPackingToteRatio(), DELTA));
    assertThat(1.000, closeTo(fifthHour.getPackingToteRatio() + fifthHour.getPackingWallRatio(), DELTA));
  }

  private void mockRatiosWhitAllMirrors() {
    final var ratioList = List.of(
        createRatio(Instant.parse("2023-01-18T16:00:00Z"), 0.625),
        createRatio(Instant.parse("2023-01-18T17:00:00Z"), 0.2),
        createRatio(Instant.parse("2023-01-18T18:00:00Z"), 0.1),
        createRatio(Instant.parse("2023-01-18T19:00:00Z"), 1.000),
        createRatio(Instant.parse("2023-01-18T20:00:00Z"), 0.75),

        createRatio(Instant.parse("2023-01-11T16:00:00Z"), 0.5),
        createRatio(Instant.parse("2023-01-11T17:00:00Z"), 0.625),
        createRatio(Instant.parse("2023-01-11T18:00:00Z"), 0.2),
        createRatio(Instant.parse("2023-01-11T19:00:00Z"), 0.1),
        createRatio(Instant.parse("2023-01-11T20:00:00Z"), 1.000),

        createRatio(Instant.parse("2023-01-04T16:00:00Z"), 0.75),
        createRatio(Instant.parse("2023-01-04T17:00:00Z"), 0.5),
        createRatio(Instant.parse("2023-01-04T18:00:00Z"), 0.625),
        createRatio(Instant.parse("2023-01-04T19:00:00Z"), 0.325),
        createRatio(Instant.parse("2023-01-04T20:00:00Z"), 0.812)
    );

    for (int i = 1; i <= DEFAULT_WEEKS; i++) {
      when(ratiosRepository.findRatiosByLogisticCenterIdAndDateBetweenAndType(
          ARTW01,
          DATE_FROM.minus(i * DAYS_IN_A_WEEK, DAYS),
          DATE_TO.minus(i * DAYS_IN_A_WEEK, DAYS),
          PW_RATIOS_TYPE
      )).thenReturn(ratioList.subList((i * 5) - 5, i * 5));
    }
  }

  private void mockRatiosWithOnlyOneMirror() {
    final var ratioList = List.of(
        createRatio(Instant.parse("2023-01-18T16:00:00Z"), 0.625),
        createRatio(Instant.parse("2023-01-18T17:00:00Z"), 0.800),

        createRatio(Instant.parse("2023-01-11T18:00:00Z"), 0.200),
        createRatio(Instant.parse("2023-01-11T19:00:00Z"), 0.100),

        createRatio(Instant.parse("2023-01-04T20:00:00Z"), 0.325),
        createRatio(Instant.parse("2022-12-28T20:00:00Z"), 0.900)
    );
    when(ratiosRepository.findRatiosByLogisticCenterIdAndDateBetweenAndType(
        ARTW01,
        DATE_FROM.minus(DAYS_IN_A_WEEK, DAYS),
        DATE_TO.minus(DAYS_IN_A_WEEK, DAYS),
        PW_RATIOS_TYPE
    )).thenReturn(ratioList.subList(0, 2));
    when(ratiosRepository.findRatiosByLogisticCenterIdAndDateBetweenAndType(
        ARTW01,
        DATE_FROM.minus(2 * DAYS_IN_A_WEEK, DAYS),
        DATE_TO.minus(2 * DAYS_IN_A_WEEK, DAYS),
        PW_RATIOS_TYPE
    )).thenReturn(ratioList.subList(2, 4));
    when(ratiosRepository.findRatiosByLogisticCenterIdAndDateBetweenAndType(
        ARTW01,
        DATE_FROM.minus(3 * DAYS_IN_A_WEEK, DAYS),
        DATE_TO.minus(3 * DAYS_IN_A_WEEK, DAYS),
        PW_RATIOS_TYPE
    )).thenReturn(ratioList.subList(4, 6));
  }

  private Ratio createRatio(final Instant date, final double value) {
    return new Ratio(Workflow.FBM_WMS_OUTBOUND, ARTW01, PW_RATIOS_TYPE, date, value);
  }
}
