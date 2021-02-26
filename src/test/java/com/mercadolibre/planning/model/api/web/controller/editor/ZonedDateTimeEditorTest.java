package com.mercadolibre.planning.model.api.web.controller.editor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ZonedDateTimeEditorTest {
    private final ZonedDateTimeEditor editor = new ZonedDateTimeEditor();

    @Test
    @DisplayName("setAsText should work")
    public void testGetOk() {
        // WHEN
        editor.setAsText("2018-04-28T10:10:10.123Z");

        // THEN
        assertAll(
                () -> assertEquals(ZonedDateTime.class, editor.getValue().getClass()),
                () -> assertEquals(
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                                ZonedDateTime.of(
                                        2018,
                                        4,
                                        28,
                                        10,
                                        10,
                                        10,
                                        123_000_000,
                                        ZoneId.of("UTC")
                                )
                        ),
                        editor.getValue().toString()
                )
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
                        "text is not a zoned date time",
                        "aaa",
                        "Value aaa should be ZonedDateTime"
                )
        );
    }
}
