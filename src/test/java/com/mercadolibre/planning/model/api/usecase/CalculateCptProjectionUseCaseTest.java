package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.domain.usecase.entities.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionOutput;
import com.mercadolibre.planning.model.api.web.controller.request.ProjectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput.builder;
import static com.mercadolibre.planning.model.api.web.controller.request.ProjectionType.BACKLOG;
import static com.mercadolibre.planning.model.api.web.controller.request.ProjectionType.CPT;
import static java.time.ZonedDateTime.parse;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
public class CalculateCptProjectionUseCaseTest {

    private static final ZonedDateTime DATE_FROM_10 = parse("2020-01-01T10:00:00Z");
    private static final ZonedDateTime DATE_IN_11 = parse("2020-01-01T11:00:00Z");
    private static final ZonedDateTime DATE_OUT_12 = parse("2020-01-01T12:00:00Z");
    private static final ZonedDateTime DATE_OUT_12_30 = parse("2020-01-01T12:30:00Z");
    private static final ZonedDateTime DATE_OUT_13 = parse("2020-01-01T13:00:00Z");
    private static final ZonedDateTime DATE_TO_14 = parse("2020-01-01T14:00:00Z");
    private static final ZonedDateTime DATE_OUT_16 = parse("2020-01-01T16:00:00Z");

    @InjectMocks
    private CalculateCptProjectionUseCase calculateCptProjection;

    @Test
    @DisplayName("The projected end date is the same as the date out and all units were processed")
    public void testSameProjectedEndDateAndDateOut() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final List<GetPlanningDistributionOutput> planningUnits = singletonList(builder()
                .dateOut(DATE_OUT_12)
                .dateIn(DATE_IN_11)
                .total(200)
                .build());

        final ProjectionInput input = ProjectionInput.builder()
                .dateFrom(DATE_FROM_10)
                .dateTo(DATE_TO_14)
                .planningUnits(planningUnits)
                .throughput(mockThroughputs(DATE_FROM_10, DATE_OUT_12, List.of(100, 200, 200)))
                .backlog(backlogs)
                .build();

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final ProjectionOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(DATE_OUT_12, projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("The projected end date is before the date out and all units were processed")
    public void testProjectedEndDateBeforeDateOut() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 100));
        final List<GetPlanningDistributionOutput> planningUnits = singletonList(builder()
                .dateOut(DATE_OUT_12)
                .dateIn(DATE_IN_11)
                .total(100)
                .build());

        final ProjectionInput input = ProjectionInput.builder()
                .dateFrom(DATE_FROM_10)
                .dateTo(DATE_TO_14)
                .planningUnits(planningUnits)
                .throughput(mockThroughputs(DATE_FROM_10, DATE_OUT_12, List.of(100, 200, 200)))
                .backlog(backlogs)
                .build();

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final ProjectionOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertEquals(parse("2020-01-01T11:30:00Z"), projection.getProjectedEndDate());
        assertEquals(0, projection.getRemainingQuantity());
    }

    @Test
    @DisplayName("The projected end date is after the date to, so it returns null")
    public void testProjectedEndDateAfterDateTo() {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(DATE_OUT_12, 1000));
        final List<GetPlanningDistributionOutput> planningUnits = singletonList(builder()
                .dateOut(DATE_OUT_12)
                .dateIn(DATE_IN_11)
                .total(100)
                .build());

        final ProjectionInput input = ProjectionInput.builder()
                .dateFrom(DATE_FROM_10)
                .dateTo(DATE_TO_14)
                .planningUnits(planningUnits)
                .throughput(mockThroughputs(DATE_FROM_10, DATE_OUT_12, List.of(100, 200, 200)))
                .backlog(backlogs)
                .build();

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final ProjectionOutput projection = projections.get(0);
        assertEquals(DATE_OUT_12, projection.getDate());
        assertNull(projection.getProjectedEndDate());
        assertEquals(800, projection.getRemainingQuantity());
    }

    @ParameterizedTest
    @DisplayName("The projected end date is after the date out and some units weren't processed")
    @MethodSource("multipleDateOuts")
    public void testProjectedEndDateAfterDateOut(final ZonedDateTime dateOut,
                                                 final int remainingQuantity) {
        // GIVEN
        final List<Backlog> backlogs = singletonList(new Backlog(dateOut, 100));
        final List<GetPlanningDistributionOutput> planningUnits = singletonList(builder()
                .dateOut(dateOut)
                .dateIn(DATE_IN_11)
                .total(400)
                .build());

        final ProjectionInput input = ProjectionInput.builder()
                .dateFrom(DATE_FROM_10)
                .dateTo(DATE_TO_14)
                .planningUnits(planningUnits)
                .throughput(mockThroughputs(DATE_FROM_10, dateOut, List.of(100, 200, 200)))
                .backlog(backlogs)
                .build();

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final ProjectionOutput projection = projections.get(0);
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

        final List<GetPlanningDistributionOutput> planningUnits = List.of(
                builder().dateOut(DATE_OUT_12).dateIn(DATE_IN_11).total(100).build(),
                builder().dateOut(DATE_OUT_13).dateIn(DATE_IN_11).total(350).build());

        final ProjectionInput input = ProjectionInput.builder()
                .dateFrom(DATE_FROM_10)
                .dateTo(DATE_OUT_16)
                .planningUnits(planningUnits)
                .throughput(mockThroughputs(DATE_FROM_10, DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 100, 100, 100, 100, 100, 100, 100)))
                .backlog(backlogs)
                .build();

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(2, projections.size());

        final ProjectionOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_12, projection1.getDate());
        assertEquals(parse("2020-01-01T11:30:00Z"), projection1.getProjectedEndDate());
        assertEquals(0, projection1.getRemainingQuantity());

        final ProjectionOutput projection2 = projections.get(1);
        assertEquals(DATE_OUT_13, projection2.getDate());
        assertEquals(parse("2020-01-01T15:00:00Z"), projection2.getProjectedEndDate());
        assertEquals(200, projection2.getRemainingQuantity());
    }

    @Test
    @DisplayName("Cpt without items shouldn't be returned")
    public void testEmptyCpt() {
        final ProjectionInput input = ProjectionInput.builder()
                .dateFrom(DATE_FROM_10)
                .dateTo(DATE_OUT_16)
                .throughput(mockThroughputs(DATE_FROM_10, DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 100, 100, 100, 100, 100, 100, 100)))
                .planningUnits(List.of(
                        builder().dateOut(DATE_OUT_12).dateIn(DATE_IN_11).total(0).build()))
                .build();

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertTrue(projections.isEmpty());
    }

    @Test
    @DisplayName("Recalculate de projection if has new items")
    public void testRecalculateProjectionDate() {
        final ProjectionInput input = ProjectionInput.builder()
                .dateFrom(DATE_FROM_10)
                .dateTo(DATE_OUT_16)
                .planningUnits(List.of(
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.minusHours(1))
                                .total(100).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11)
                                .total(100).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(1))
                                .total(50).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(2))
                                .total(50).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(3))
                                .total(50).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(4))
                                .total(50).build(),
                        builder().dateOut(DATE_OUT_16).dateIn(DATE_IN_11.plusHours(5))
                                .total(50).build()))
                .throughput(mockThroughputs(DATE_FROM_10, DATE_OUT_16.plusHours(2),
                        List.of(200, 200, 20, 20, 20, 20, 20, 20, 20)))
                .build();

        // WHEN
        final List<ProjectionOutput> projections = calculateCptProjection.execute(input);

        // THEN
        assertEquals(1, projections.size());

        final ProjectionOutput projection1 = projections.get(0);
        assertEquals(DATE_OUT_16, projection1.getDate());
        assertNull(projection1.getProjectedEndDate());
        assertEquals(120, projection1.getRemainingQuantity());
    }

    @ParameterizedTest
    @DisplayName("Only supports cpt type")
    @MethodSource("getSupportedProjectionTypes")
    public void testSupportEntityTypeOk(final ProjectionType projectionType,
                                        final boolean shouldBeSupported) {
        // WHEN
        final boolean isSupported = calculateCptProjection.supportsProjectionType(projectionType);

        // THEN
        assertEquals(shouldBeSupported, isSupported);
    }

    private static Stream<Arguments> getSupportedProjectionTypes() {
        return Stream.of(
                Arguments.of(CPT, true),
                Arguments.of(BACKLOG, false)
        );
    }

    private List<EntityOutput> mockThroughputs(final ZonedDateTime dateFrom,
                                               final Temporal dateTo,
                                               final List<Integer> values) {

        final List<EntityOutput> entityOutputs = new ArrayList<>();

        for (int i = 0; i <= HOURS.between(dateFrom, dateTo); i++) {
            entityOutputs.add(
                    EntityOutput.builder()
                            .date(dateFrom.plusHours(i))
                            .value(values.get(i))
                            .build());
        }

        return entityOutputs;
    }
}
