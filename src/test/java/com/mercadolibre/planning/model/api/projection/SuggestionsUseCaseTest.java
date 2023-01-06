package com.mercadolibre.planning.model.api.projection;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SuggestionsUseCaseTest {
    private static final Instant VIEW_DATE1430 = Instant.parse("2022-12-05T14:30:00Z");
    private static final Instant DATE_OUT1 = Instant.parse("2022-12-05T15:00:00Z");
    private static final Instant DATE_OUT1530 = Instant.parse("2022-12-05T15:30:00Z");
    private static final Instant DATE_OUT3 = Instant.parse("2022-12-05T20:00:00Z");

    @Test
    void testForHappyCase() {
        // GIVEN
        final SuggestionsUseCase suggestionsUseCase = new SuggestionsUseCase();
        final List<Suggestion> suggestedWaves = suggestionsUseCase.execute(
                getListOfProcessPaths(),
                getListOfDummyBacklog(),
                getRatios(),
                getBacklogsLimits(),
                VIEW_DATE1430
        );
        var expectedSuggestions = getExpectedSuggestionForClosenessHappyCase().get(0);
        var suggested = suggestedWaves.get(0);
        // THEN
        Assertions.assertNotNull(suggestedWaves);
        Assertions.assertEquals(getExpectedSuggestionForClosenessSLA().size(), suggestedWaves.size());
        Assertions.assertTrue(assertEqualsQuantities(expectedSuggestions.getExpectedQuantities(), suggested.getExpectedQuantities()));
        Assertions.assertTrue(assertEqualsProcessPath(expectedSuggestions.getWaves(), suggested.getWaves()));
        Assertions.assertEquals(expectedSuggestions.getDate(), suggested.getDate());
        Assertions.assertEquals(expectedSuggestions.getReason(), suggested.getReason());
    }

    @Test
    void testForHighValueAtLowerBounds() {
        // WHEN
        final SuggestionsUseCase suggestionUseCase = new SuggestionsUseCase();
        final List<Suggestion> suggestedWaves = suggestionUseCase.execute(
                getListOfProcessPaths(),
                getListOfBacklog(),
                getRatios(),
                getBacklogsLimits(),
                VIEW_DATE1430);
        final Suggestion expectedSuggestions = getExpectedSuggestionForClosenessSLA().get(0);
        final Suggestion suggested = suggestedWaves.get(0);

        // THEN
        Assertions.assertNotNull(suggestedWaves);
        Assertions.assertEquals(getExpectedSuggestionForClosenessSLA().size(), suggestedWaves.size());
        Assertions.assertTrue(assertEqualsQuantities(expectedSuggestions.getExpectedQuantities(), suggested.getExpectedQuantities()));
        Assertions.assertTrue(assertEqualsProcessPath(expectedSuggestions.getWaves(), suggested.getWaves()));
        Assertions.assertEquals(expectedSuggestions.getDate(), suggested.getDate());
        Assertions.assertEquals(expectedSuggestions.getReason(), suggested.getReason());
    }

    @Test
    public void testGetSuggestedWavesWithZeroValueAtUpperBounds() {
        // GIVEN
        SuggestionsUseCase suggestionsUseCase = new SuggestionsUseCase();
        // WHEN
        List<Suggestion> suggestedWaves = suggestionsUseCase.execute(
                getListOfProcessPaths(),
                getListOfBacklog(),
                getRatios(),
                getBacklogsLimitsWithZeroValue(),
                VIEW_DATE1430
        );
        final Suggestion expectedSuggestions = getExpectedSuggestionForClosenessSLA().get(0);
        final Suggestion suggested = suggestedWaves.get(0);

        // THEN
        Assertions.assertNotNull(suggestedWaves);
        Assertions.assertEquals(getExpectedSuggestionForClosenessSLA().size(), suggestedWaves.size());
        Assertions.assertTrue(assertEqualsQuantities(expectedSuggestions.getExpectedQuantities(), suggested.getExpectedQuantities()));
        Assertions.assertTrue(assertEqualsProcessPath(expectedSuggestions.getWaves(), suggested.getWaves()));
        Assertions.assertEquals(expectedSuggestions.getDate(), suggested.getDate());
        Assertions.assertEquals(expectedSuggestions.getReason(), suggested.getReason());
    }

    private boolean assertEqualsProcessPath(final List<Wave> expectedWave, final List<Wave> suggestedWave) {
        final List<Wave> equalsProcessPath = expectedWave.stream()
                .filter(suggestedWave::contains)
                .collect(Collectors.toList());
        return equalsProcessPath.size() == expectedWave.size();
    }

    private Map<ProcessPath, Map<Instant, Float>> getRatios() {
        return Map.of(
                ProcessPath.TOT_MONO, Map.of(VIEW_DATE1430, 0.25F),
                ProcessPath.TOT_MULTI_BATCH, Map.of(VIEW_DATE1430, 0.25F),
                ProcessPath.TOT_MULTI_ORDER, Map.of(VIEW_DATE1430, 0.25F),
                ProcessPath.NON_TOT_MONO, Map.of(VIEW_DATE1430, 0.25F)
        );
    }

    private Map<ProcessName, Map<Instant, Integer>> getBacklogsLimits() {
        return Map.of(
                ProcessName.PICKING, Map.of(VIEW_DATE1430, 10000, VIEW_DATE1430.plus(5, ChronoUnit.HOURS), 50000),
                ProcessName.PACKING, Map.of(VIEW_DATE1430, 2000),
                ProcessName.BATCH_SORTER, Map.of(VIEW_DATE1430, 700)
        );
    }

    private Map<ProcessName, Map<Instant, Integer>> getBacklogsLimitsWithZeroValue() {
        return Map.of(
                ProcessName.PICKING, Map.of(VIEW_DATE1430, 10000),
                ProcessName.PACKING, Map.of(VIEW_DATE1430, 0),
                ProcessName.BATCH_SORTER, Map.of(VIEW_DATE1430, 700)
        );
    }

    private boolean assertEqualsQuantities(final List<UnitsByDateOut> expected, final List<UnitsByDateOut> obtained) {
        final List<UnitsByDateOut> expectedQuantities = expected.stream()
                .filter(obtained::contains)
                .collect(Collectors.toList());
        return expectedQuantities.size() == expected.size();
    }

    private List<ProcessPathConfiguration> getListOfProcessPaths() {
        return List.of(
                        new ProcessPathConfiguration(ProcessPath.NON_TOT_MONO, 120, 60, 60),
                        new ProcessPathConfiguration(ProcessPath.TOT_MULTI_BATCH, 200, 120, 30),
                        new ProcessPathConfiguration(ProcessPath.TOT_MULTI_ORDER, 120, 60, 30)
                );
    }

    private List<UnitsByProcessPathAndProcess> getListOfBacklog() {
        return List.of(
                new UnitsByProcessPathAndProcess(ProcessPath.NON_TOT_MONO, ProcessName.WAVING, DATE_OUT1530, 2000),
                new UnitsByProcessPathAndProcess(ProcessPath.NON_TOT_MONO, ProcessName.WAVING, DATE_OUT1530, 5000),
                new UnitsByProcessPathAndProcess(ProcessPath.NON_TOT_MONO, ProcessName.WAVING, DATE_OUT3, 2000),
                new UnitsByProcessPathAndProcess(ProcessPath.TOT_MULTI_BATCH, ProcessName.WAVING, DATE_OUT1, 8000),
                new UnitsByProcessPathAndProcess(ProcessPath.TOT_MULTI_BATCH, ProcessName.PACKING, DATE_OUT1, 8000),
                new UnitsByProcessPathAndProcess(ProcessPath.TOT_MULTI_BATCH, ProcessName.PACKING_WALL, DATE_OUT1, 8000)

        );
    }

    private List<UnitsByProcessPathAndProcess> getListOfDummyBacklog() {
        return List.of(
                new UnitsByProcessPathAndProcess(ProcessPath.NON_TOT_MONO, ProcessName.WAVING, DATE_OUT1530, 100),
                new UnitsByProcessPathAndProcess(ProcessPath.NON_TOT_MONO, ProcessName.WAVING, DATE_OUT3, 200),
                new UnitsByProcessPathAndProcess(ProcessPath.TOT_MULTI_BATCH, ProcessName.WAVING, DATE_OUT1, 80)

        );
    }

    private List<Suggestion> getExpectedSuggestionForClosenessSLA() {
        return List.of(
                new Suggestion(
                        VIEW_DATE1430,
                        List.of(
                                new Wave(ProcessPath.NON_TOT_MONO, 2500, 2500, new TreeSet<>(Collections.singleton(DATE_OUT1530))),
                                new Wave(ProcessPath.TOT_MULTI_BATCH, 700, 700, new TreeSet<>(Collections.singleton(DATE_OUT1)))

                        ),
                        TriggerName.SLA,
                        List.of(
                                new UnitsByDateOut(DATE_OUT1, 8000),
                                new UnitsByDateOut(DATE_OUT1530, 7000)
                        )
                )
        );
    }

    private List<Suggestion> getExpectedSuggestionForClosenessHappyCase() {
        return List.of(
                new Suggestion(
                        VIEW_DATE1430,
                        List.of(
                                new Wave(ProcessPath.NON_TOT_MONO, 100, 2500, new TreeSet<>(Collections.singleton(DATE_OUT1530))),
                                new Wave(ProcessPath.TOT_MULTI_BATCH, 80, 700, new TreeSet<>(Collections.singleton(DATE_OUT1)))

                        ),
                        TriggerName.SLA,
                        List.of(
                                new UnitsByDateOut(DATE_OUT1530, 100),
                                new UnitsByDateOut(DATE_OUT1, 80)
                        )
                )
        );
    }
}
