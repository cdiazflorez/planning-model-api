package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.web.controller.request.Source;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SourceEditorTest {

    private final SourceEditor editor = new SourceEditor();

    @Test
    @DisplayName("setAsText should work")
    public void testGetOk() {
        // WHEN
        editor.setAsText(Source.FORECAST.name());

        // THEN
        assertAll(
                () -> assertEquals(Source.class, editor.getValue().getClass()),
                () -> assertEquals(Source.FORECAST, editor.getValue())
        );
    }

    @DisplayName("setAsText should failed because")
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidText")
    public void testTextIsBlank(final String title,
                                final String text,
                                final String expectedMessage) {
        // WHEN
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> editor.setAsText(text)
        );

        // THEN
        assertEquals(expectedMessage, exception.getMessage());
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<Arguments> invalidText() {
        return Stream.of(
                Arguments.of(
                        "text is blank",
                        "",
                        "Value should not be blank"
                ),
                Arguments.of(
                        "text is not a source",
                        "aaa",
                        format("Value aaa should be one of %s",
                                Arrays.toString(Source.values()))
                )
        );
    }
}
