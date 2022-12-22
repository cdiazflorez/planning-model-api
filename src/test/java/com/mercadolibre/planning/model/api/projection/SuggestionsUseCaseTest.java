package com.mercadolibre.planning.model.api.projection;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SuggestionsUseCaseTest {
    private static final Instant VIEW_DATE1430 = Instant.parse("2022-12-05T14:30:00Z");
    private static final Instant DATE_OUT1 = Instant.parse("2022-12-05T15:00:00Z");
    private static final Instant DATE_OUT1530 = Instant.parse("2022-12-05T15:30:00Z");
    private static final Instant DATE_OUT3 = Instant.parse("2022-12-05T20:00:00Z");

    @Test
    void happyCase() {
        // WHEN
        var suggestionUC = new SuggestionsUseCase();
        var suggestedWaves = suggestionUC.execute(getListOfProcessPaths(), getListOfBacklog(), VIEW_DATE1430);

        // THEN
        final var expectedSuggestions = getExpectedSuggestionForClosenessSLA().get(0);
        final var suggested = suggestedWaves.get(0);

        Assertions.assertNotNull(suggestedWaves);
        Assertions.assertEquals(getExpectedSuggestionForClosenessSLA().size(), suggestedWaves.size());
        Assertions.assertTrue(assertEqualsQuantities(expectedSuggestions.getExpectedQuantities(), suggested.getExpectedQuantities()));
        Assertions.assertTrue(assertEqualsProcessPath(expectedSuggestions.getProcessPath(), suggested.getProcessPath()));
        Assertions.assertEquals(expectedSuggestions.getDate(), suggested.getDate());
        Assertions.assertEquals(expectedSuggestions.getReason(), suggested.getReason());
    }

    private boolean assertEqualsProcessPath(final List<BoundsByPPath> processPath, final List<BoundsByPPath> processPath1) {
        final List<BoundsByPPath> equalsProcessPath = processPath.stream().filter(processPath1::contains).collect(Collectors.toList());
        return equalsProcessPath.size() == processPath.size();
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

    private List<BacklogByPPathAndProcess> getListOfBacklog() {
        return List.of(
                new BacklogByPPathAndProcess(ProcessPath.NON_TOT_MONO, ProcessName.WAVING, DATE_OUT1530, 2000),
                new BacklogByPPathAndProcess(ProcessPath.NON_TOT_MONO, ProcessName.WAVING, DATE_OUT1530, 5000),
                new BacklogByPPathAndProcess(ProcessPath.NON_TOT_MONO, ProcessName.WAVING, DATE_OUT3, 2000),
                new BacklogByPPathAndProcess(ProcessPath.TOT_MULTI_BATCH, ProcessName.WAVING, DATE_OUT1, 8000)

        );
    }

    private List<Suggestion> getExpectedSuggestionForClosenessSLA() {
        return List.of(
                new Suggestion(
                        VIEW_DATE1430,
                        List.of(
                                new BoundsByPPath(ProcessPath.NON_TOT_MONO, 7000, 15000),
                                new BoundsByPPath(ProcessPath.TOT_MULTI_BATCH, 8000, 15000)

                        ),
                        TriggerName.SLA,
                        List.of(
                                new UnitsByDateOut(DATE_OUT1530, 7000),
                                new UnitsByDateOut(DATE_OUT1, 8000)
                        )
                )
        );
    }
}
