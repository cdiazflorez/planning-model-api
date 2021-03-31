package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastIdView;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastRepository;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetForecastUseCaseTest {
    @Mock
    private ForecastRepository forecastRepository;

    @InjectMocks
    private GetForecastUseCase getForecastUseCase;

    @Test
    @DisplayName("Get Forecast by warehouse, workflow and weeks OK")
    public void testGetForecastOK() {
        // GIVEN
        final GetForecastInput input = GetForecastInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC.plusDays(1))
                .build();

        when(forecastRepository.findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND.name(),
                Set.of("33-2020")
        )).thenReturn(mockForecastIdView(2));

        // WHEN
        final List<Long> forecasts = getForecastUseCase.execute(input);

        // THEN
        assertFalse(forecasts.isEmpty());
        assertEquals(2, forecasts.size());

        final Long forecastId1 = forecasts.get(0);
        assertEquals(Long.valueOf(0), forecastId1);

        final Long forecastId2 = forecasts.get(1);
        assertEquals(Long.valueOf(1), forecastId2);
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

        when(forecastRepository.findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND.name(),
                weeks
        )).thenReturn(mockForecastIdView(weeks.size()));

        // WHEN
        final List<Long> forecasts = getForecastUseCase.execute(input);

        // THEN
        assertFalse(forecasts.isEmpty());
        assertEquals(weeks.size(), forecasts.size());

        IntStream.range(0, weeks.size())
                .forEach(i -> assertEquals(Long.valueOf(i), forecasts.get(i)));
    }

    private static Stream<Arguments> datesAndWeeks() {
        return Stream.of(
                arguments(
                        // From friday to saturday
                        ZonedDateTime.of(2021, 4, 2, 4, 0, 0, 0, ZoneId.of("UTC")),
                        ZonedDateTime.of(2021, 4, 3, 4, 0, 0, 0, ZoneId.of("UTC")),
                        Set.of("13-2021")),
                arguments(
                        // From saturday to sunday
                        ZonedDateTime.of(2021, 4, 3, 6, 0, 0, 0, ZoneId.of("UTC")),
                        ZonedDateTime.of(2021, 4, 4, 4, 0, 0, 0, ZoneId.of("UTC")),
                        Set.of("13-2021", "14-2021")),
                arguments(
                        // From sunday to monday
                        ZonedDateTime.of(2021, 4, 4, 4, 0, 0, 0, ZoneId.of("UTC")),
                        ZonedDateTime.of(2021, 4, 5, 4, 0, 0, 0, ZoneId.of("UTC")),
                        Set.of("14-2021"))
        );
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

        when(forecastRepository.findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND.name(),
                getForecastWeeks(input.getDateFrom(), input.getDateTo())
        )).thenReturn(List.of());

        // WHEN - THEN
        assertThrows(ForecastNotFoundException.class, () -> getForecastUseCase.execute(input));
    }

    private static List<ForecastIdView> mockForecastIdView(final int size) {
        final List<ForecastIdView> forecastIdViews = new ArrayList<>();
        LongStream.range(0, size).forEach(i -> forecastIdViews.add(new ForecastIdViewImpl(i)));
        return forecastIdViews;
    }
}
