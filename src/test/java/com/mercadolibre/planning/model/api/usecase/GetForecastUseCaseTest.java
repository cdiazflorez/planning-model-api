package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastIdView;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastRepository;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetForecastUseCaseTest {

  private static final Instant NOW = Instant.parse("2022-09-01T00:00:00Z");

  @Mock
  private ForecastRepository forecastRepository;

  private GetForecastUseCase getForecastUseCase;

  private MockedStatic<Instant> now;

  private static Stream<Arguments> datesAndWeeks() {
    return Stream.of(
        arguments(
            // From friday to saturday
            ZonedDateTime.of(2021, 4, 2, 4, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2021, 4, 3, 4, 0, 0, 0, ZoneId.of("UTC")),
            Set.of("13-2021", "14-2021")),
        arguments(
            // From saturday to sunday
            ZonedDateTime.of(2021, 4, 3, 6, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2021, 4, 4, 4, 0, 0, 0, ZoneId.of("UTC")),
            Set.of("13-2021", "14-2021")),
        arguments(
            // From sunday to monday
            ZonedDateTime.of(2021, 4, 4, 4, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2021, 4, 5, 4, 0, 0, 0, ZoneId.of("UTC")),
            Set.of("13-2021", "14-2021")),
        arguments(
            // From saturday to sunday with a distance less than one hour
            ZonedDateTime.of(2021, 4, 3, 23, 10, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2021, 4, 4, 0, 2, 0, 0, ZoneId.of("UTC")),
            Set.of("13-2021", "14-2021")),
        arguments(
            // Between two dates with a distance less than one hour
            ZonedDateTime.of(2021, 4, 4, 10, 10, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2021, 4, 4, 10, 50, 0, 0, ZoneId.of("UTC")),
            Set.of("13-2021", "14-2021")),
        arguments(
            // Saturday at 9PM on Buenos Aires but 12AM on UTC
            ZonedDateTime.of(2021, 4, 11, 0, 10, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2021, 4, 11, 1, 0, 0, 0, ZoneId.of("UTC")),
            Set.of("14-2021", "15-2021")),
        arguments(
            // New year's eve
            ZonedDateTime.of(2021, 12, 31, 23, 10, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")),
            Set.of("1-2022"))
    );
  }

  private static List<ForecastIdView> mockForecastIdView(final int size) {
    final List<ForecastIdView> forecastIdViews = new ArrayList<>();
    LongStream.range(0, size).forEach(i -> forecastIdViews.add(new ForecastIdViewImpl(i)));
    return forecastIdViews;
  }

  @BeforeEach
  public void setUp() {
    now = mockStatic(Instant.class);
    now.when(Instant::now).thenReturn(NOW);
    getForecastUseCase = new GetForecastUseCase(forecastRepository, new GetForecastUseCase.RequestScopedMemory());
  }

  @AfterEach
  public void tearDown() {
    now.close();
  }

  @ParameterizedTest
  @MethodSource("datesAndWeeks")
  @DisplayName("Get Forecast by warehouse, workflow and weeks OK everyday")
  public void testGetForecastEverydayOK(final ZonedDateTime dateFrom,
                                        final ZonedDateTime dateTo,
                                        final Set<String> weeks) {

    // GIVEN
    final GetForecastInput input = GetForecastInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .build();

    when(forecastRepository.findForecastIdsByWarehouseIdAAndWorkflowAndWeeksAtViewDate(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND.name(),
        weeks,
        NOW
    )).thenReturn(mockForecastIdView(weeks.size()));

    // WHEN
    final List<Long> forecasts = getForecastUseCase.execute(input);

    // THEN
    assertFalse(forecasts.isEmpty());
    assertEquals(weeks.size(), forecasts.size());

    IntStream.range(0, weeks.size())
        .forEach(i -> assertEquals(Long.valueOf(i), forecasts.get(i)));
  }

  @Test
  @DisplayName("When no forecast is present an exception must be thrown")
  public void testThrowExceptionWhenForecastNotFound() {
    // GIVEN
    final GetForecastInput input = GetForecastInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(A_DATE_UTC)
        .dateTo(A_DATE_UTC.plusDays(1))
        .build();

    when(forecastRepository.findForecastIdsByWarehouseIdAAndWorkflowAndWeeksAtViewDate(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND.name(),
        getForecastWeeks(input.getDateFrom(), input.getDateTo()),
        NOW
    )).thenReturn(List.of());

    // WHEN - THEN
    assertThrows(ForecastNotFoundException.class, () -> getForecastUseCase.execute(input));
  }

  @Test
  void testShouldSearchForecastByViewDateIfViewDateIsPresentInInput() {
    // GIVEN
    final var someInstant = Instant.now();
    final var weeks = getForecastWeeks(A_DATE_UTC, A_DATE_UTC.plusDays(1));

    final GetForecastInput input = GetForecastInput.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .dateFrom(A_DATE_UTC)
        .dateTo(A_DATE_UTC.plusDays(1))
        .viewDate(someInstant)
        .build();

    when(forecastRepository.findForecastIdsByWarehouseIdAAndWorkflowAndWeeksAtViewDate(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND.name(),
        weeks,
        someInstant
    )).thenReturn(mockForecastIdView(weeks.size()));

    // WHEN
    getForecastUseCase.execute(input);
  }
}
