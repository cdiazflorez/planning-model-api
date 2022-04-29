package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.exception.InvalidProjectionTypeException;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProjectionTypeEditorTest {

    private final ProjectionTypeEditor editor = new ProjectionTypeEditor();

    @Test
    @DisplayName("setAsText should work")
    public void testGetOk() {
        // WHEN
        editor.setAsText(ProjectionType.CPT.name());

        // THEN
        assertAll(
                () -> assertEquals(ProjectionType.class, editor.getValue().getClass()),
                () -> assertEquals(ProjectionType.CPT, editor.getValue())
        );
    }

    @Test
    @DisplayName("setAsText should failed because is blank")
    public void testTextIsBlank() {
        // WHEN
        final IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> editor.setAsText("")
        );

        // THEN
        assertEquals("Value should not be blank", exception.getMessage());
    }

    @Test
    @DisplayName("setAsText should failed because is not a projection type")
    public void testTextIsInvalid() {
        // WHEN
        final InvalidProjectionTypeException exception = assertThrows(
                InvalidProjectionTypeException.class,
                () -> editor.setAsText("aaa")
        );

        // THEN
        assertEquals("Value aaa is invalid, instead it should be one of"
                + " [BACKLOG, CPT, DEFERRAL, COMMAND_CENTER_DEFERRAL]", exception.getMessage());
    }
}
