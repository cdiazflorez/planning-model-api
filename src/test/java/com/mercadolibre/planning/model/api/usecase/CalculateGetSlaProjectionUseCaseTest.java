package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.SlaProjectionInput;
import com.mercadolibre.planning.model.api.util.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static java.time.ZonedDateTime.parse;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class CalculateGetSlaProjectionUseCaseTest {

    private static final ZonedDateTime DATE_10 = parse("2020-01-01T10:00:00Z");
    private static final ZonedDateTime DATE_IN_11 = parse("2020-01-01T11:00:00Z");
    private static final ZonedDateTime DATE_OUT_12 = parse("2020-01-01T12:00:00Z");
    private static final ZonedDateTime DATE_OUT_12_30 = parse("2020-01-01T12:30:00Z");
    private static final ZonedDateTime DATE_OUT_13 = parse("2020-01-01T13:00:00Z");
    private static final ZonedDateTime DATE_TO_14 = parse("2020-01-01T14:00:00Z");
    private static final ZonedDateTime DATE_OUT_16 = parse("2020-01-01T16:00:00Z");

    private MockedStatic<DateUtils> mockedDates;

    @BeforeEach
    public void setUp() {
        mockedDates = mockStatic(DateUtils.class);
        mockedDates.when(DateUtils::getCurrentUtcDate).thenReturn(DATE_10);
        mockedDates.when(() -> DateUtils.ignoreMinutes(any())).thenCallRealMethod();
    }

    @AfterEach
    public void tearDown() {
        mockedDates.close();
    }

    @Test
    @DisplayName("The projected end date is the same as the date out and all units were processed")
    public void testSameProjectedEndDateAndDateOut() {

        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));

        final List<PlannedUnits> plannedUnits = singletonList(new PlannedUnits(DATE_IN_11, DATE_OUT_12, 200));

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_TO_14)
                .plannedUnits(plannedUnits)
                .capacity(mockCapacity(DATE_OUT_12, List.of(-1, 100, 200)))
                .logisticCenterId(WAREHOUSE_ID)
                .backlog(backlogs)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(1, projections.size());
        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(DATE_OUT_13, projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("The projected end date is before the date out and all units were processed")
    public void testProjectedEndDateBeforeDateOut() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final List<PlannedUnits> plannedUnits = singletonList(new PlannedUnits(DATE_IN_11, DATE_OUT_12, 100));

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_TO_14)
                .plannedUnits(plannedUnits)
                .capacity(mockCapacity(DATE_OUT_12, List.of(100, 200, 200)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(parse("2020-01-01T12:30:00Z"), projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("The projected end date is after the date to, so it returns null")
    public void testProjectedEndDateAfterDateTo() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 1000));
        final List<PlannedUnits> plannedUnits = singletonList(new PlannedUnits(DATE_IN_11, DATE_OUT_12, 100));

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_TO_14)
                .plannedUnits(plannedUnits)
                .capacity(mockCapacity(DATE_OUT_12, List.of(100, 200, 200)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertNull(projection.getProjectedEndDate());
        assertEquals(700, projection.getRemainingQuantity());
    }

    @ParameterizedTest
    @DisplayName("The projected end date is after the date out and some units weren't processed")
    @MethodSource("multipleDateOuts")
    public void testProjectedEndDateAfterDateOut(final ZonedDateTime dateOut,
                                                 final int remainingQuantity) {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(dateOut, 100));
        final List<PlannedUnits> plannedUnits = singletonList(new PlannedUnits(DATE_10, dateOut, 400));

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_TO_14)
                .plannedUnits(plannedUnits)
                .capacity(mockCapacity(dateOut, List.of(100, 200, 200)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(dateOut).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection = projections.get(0);
        assertEquals(dateOut, projection.getDate());
        assertEquals(parse("2020-01-01T13:00:00Z"), projection.getProjectedEndDate());
        assertEquals(remainingQuantity, projection.getRemainingQuantity());
    }

    private static Stream<Arguments> multipleDateOuts() {
        return Stream.of(
                arguments(DATE_OUT_12, 200),
                arguments(DATE_OUT_12_30, 100)
        );
    }

    @Test
    @DisplayName("The capacity is shared among al date outs")
    public void testMultipleDateOuts() {
        // GIVEN
        final List<Backlog> backlogs = List.of(
                new Backlog(DATE_OUT_12, 100),
                new Backlog(DATE_OUT_13, 150)
        );

        final List<PlannedUnits> plannedUnits = List.of(
                new PlannedUnits(DATE_IN_11, DATE_OUT_12, 100),
                new PlannedUnits(DATE_IN_11, DATE_OUT_13, 350)
        );

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .plannedUnits(plannedUnits)
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 200, 100, 100, 100, 100, 100, 100)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12).build(),
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_13).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(2, projections.size());

        final CptCalculationOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_12, projection1.getDate());
        assertEquals(parse("2020-01-01T12:30:00Z"), projection1.getProjectedEndDate());
        assertEquals(0, projection1.getRemainingQuantity());

        final CptCalculationOutput projection2 = projections.get(1);
        assertEquals(DATE_OUT_13, projection2.getDate());
        assertEquals(parse("2020-01-01T15:30:00Z"), projection2.getProjectedEndDate());
        assertEquals(250, projection2.getRemainingQuantity());
    }

    @Test
    @DisplayName("GetCptByWarehouseOutput without items shouldn't be returned")
    public void testEmptyCpt() {
        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 100, 100, 100, 100, 100, 100, 100)))
                .plannedUnits(emptyList())
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(emptyList())
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertTrue(projections.isEmpty());
    }

    @Test
    @DisplayName("Recalculate de projection if has new items")
    public void testRecalculateProjectionDate() {
        final List<PlannedUnits> plannedUnits = List.of(
                new PlannedUnits(DATE_IN_11.minusHours(1), DATE_OUT_16, 100),
                new PlannedUnits(DATE_IN_11, DATE_OUT_16, 100),
                new PlannedUnits(DATE_IN_11.plusHours(1), DATE_OUT_16, 50),
                new PlannedUnits(DATE_IN_11.plusHours(2), DATE_OUT_16, 50),
                new PlannedUnits(DATE_IN_11.plusHours(3), DATE_OUT_16, 50),
                new PlannedUnits(DATE_IN_11.plusHours(4), DATE_OUT_16, 50),
                new PlannedUnits(DATE_IN_11.plusHours(5), DATE_OUT_16, 50)
        );

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .plannedUnits(plannedUnits)
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 20, 20, 20, 20, 20, 20, 20)))
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_16).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_16, projection1.getDate());
        assertNull(projection1.getProjectedEndDate());
        assertEquals(170, projection1.getRemainingQuantity());
    }

    @Test
    @DisplayName("CPT coming from backlog but not present in Forecast should be calculated anyway")
    public void testCptIsNotPresentInForecast() {
        // GIVEN
        final List<Backlog> backlogs = List.of(
                new Backlog(DATE_OUT_12, 100),
                new Backlog(DATE_OUT_12_30, 150),
                new Backlog(DATE_OUT_13, 200)
        );

        final List<PlannedUnits> plannedUnits = List.of(
                new PlannedUnits(DATE_IN_11, DATE_OUT_12, 100),
                new PlannedUnits(DATE_IN_11, DATE_OUT_13, 350)
        );

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .plannedUnits(plannedUnits)
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 200, 100, 100, 100, 100, 100, 100)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12).build(),
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12_30).build(),
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_13).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(3, projections.size());

        final CptCalculationOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_12, projection1.getDate());
        assertEquals(parse("2020-01-01T12:30:00Z"), projection1.getProjectedEndDate());
        assertEquals(0, projection1.getRemainingQuantity());

        final CptCalculationOutput projection2 = projections.get(1);
        assertEquals(DATE_OUT_12_30, projection2.getDate());
        assertEquals(parse("2020-01-01T11:15:00Z"), projection2.getProjectedEndDate());
        assertEquals(0, projection2.getRemainingQuantity());

        final CptCalculationOutput projection3 = projections.get(2);
        assertEquals(DATE_OUT_13, projection3.getDate());
        assertEquals(parse("2020-01-01T16:00:00Z"), projection3.getProjectedEndDate());
        assertEquals(300, projection3.getRemainingQuantity());
    }

    @Test
    @DisplayName("CPT is deferred")
    public void testCptIsDeferred() {
        // GIVEN
        final List<Backlog> backlogs = List.of(
                new Backlog(DATE_OUT_12, 100),
                new Backlog(DATE_OUT_12_30, 150),
                new Backlog(DATE_OUT_13, 200)
        );

        final List<PlannedUnits> plannedUnits = List.of(
                new PlannedUnits(DATE_IN_11, DATE_OUT_12, 100),
                new PlannedUnits(DATE_IN_11, DATE_OUT_13, 350),
                new PlannedUnits(DATE_IN_11, DATE_OUT_13, 350)
        );

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(DATE_OUT_16)
                .plannedUnits(plannedUnits)
                .capacity(mockCapacity(DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 200, 100, 100, 100, 100, 100, 100)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12).build(),
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12_30).build(),
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_13).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(3, projections.size());

        final CptCalculationOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_12, projection1.getDate());

        final CptCalculationOutput projection2 = projections.get(1);
        assertEquals(DATE_OUT_12_30, projection2.getDate());

        final CptCalculationOutput projection3 = projections.get(2);
        assertEquals(DATE_OUT_13, projection3.getDate());
    }

    @Test
    @DisplayName("Get Deferral Projection's remaining quantity OK")
    public void testDeferralProjectionOk() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final ZonedDateTime date13 = DATE_10.plusHours(3);

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(date13)
                .plannedUnits(emptyList())
                .capacity(mockCapacity(date13, List.of(50, 50, 25, 40)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(DATE_OUT_12, projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("Get Projection's all cpt")
    public void testAllCpt() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final ZonedDateTime date13 = DATE_10.plusHours(3);

        final List<PlannedUnits> plannedUnits = List.of(
                new PlannedUnits(DATE_OUT_12, DATE_OUT_12, 0),
                new PlannedUnits(DATE_OUT_12_30, DATE_OUT_12_30, 0),
                new PlannedUnits(DATE_OUT_13, DATE_OUT_13, 0)
        );

        final SlaProjectionInput input = SlaProjectionInput.builder()
                .dateFrom(DATE_10)
                .dateTo(date13)
                .plannedUnits(plannedUnits)
                .capacity(mockCapacity(date13, List.of(50, 50, 25, 40)))
                .backlog(backlogs)
                .logisticCenterId(WAREHOUSE_ID)
                .slaByWarehouse(List.of(
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12).build(),
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_12_30).build(),
                        GetSlaByWarehouseOutput.builder().date(DATE_OUT_13).build()))
                .currentDate(getCurrentUtcDate())
                .build();

        // WHEN
        final List<CptCalculationOutput> projections = CalculateCptProjectionUseCase.execute(input);

        // THEN
        assertEquals(3, projections.size());

        final CptCalculationOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(DATE_OUT_12, projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    private Map<ZonedDateTime, Integer> mockCapacity(final Temporal dateTo,
                                                     final List<Integer> values) {

        final Map<ZonedDateTime, Integer> capacity = new TreeMap<>();

        for (int i = 0; i <= HOURS.between(DATE_10, dateTo); i++) {
            if (values.get(i) >= 0) {
                capacity.put(DATE_10.plusHours(i), values.get(i));
            }
        }
        return capacity;
    }
}
