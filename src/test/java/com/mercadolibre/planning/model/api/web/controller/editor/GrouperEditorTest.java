package com.mercadolibre.planning.model.api.web.controller.editor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class GrouperEditorTest {

  private final GrouperEditor editor = new GrouperEditor();

  @SuppressWarnings("PMD.UnusedPrivateMethod")
  private static Stream<Arguments> invalidText() {
    return Stream.of(
        Arguments.of(
            "text is blank",
            "Value text is blank should be one of [DATE_OUT, DATE_IN, PROCESS_PATH]",
            "Value should not be blank"
        )
    );
  }

  @Test
  @DisplayName("setAsText should work")
  public void testGetOk() {
    // WHEN
    editor.setAsText(Grouper.DATE_IN.name());

    // THEN
    assertAll(
        () -> assertEquals(Grouper.class, editor.getValue().getClass()),
        () -> assertEquals(Grouper.DATE_IN, editor.getValue())
    );
  }

  @DisplayName("setAsText should failed because")
  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidText")
  public void testTextIsBlank(final String text, final String expectedMessage) {
    // WHEN
    final IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> editor.setAsText(text)
    );

    // THEN
    assertEquals(expectedMessage, exception.getMessage());
  }
}
