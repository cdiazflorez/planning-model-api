package com.mercadolibre.planning.model.api.web.controller.editor;

import com.mercadolibre.planning.model.api.exception.InvalidEntityTypeException;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityTypeEditorTest {
    private final EntityTypeEditor editor = new EntityTypeEditor();

    @Test
    @DisplayName("setAsText should work")
    public void testGetOk() {
        // WHEN
        editor.setAsText(EntityType.HEADCOUNT.name());

        // THEN
        assertAll(
                () -> assertEquals(EntityType.class, editor.getValue().getClass()),
                () -> assertEquals(EntityType.HEADCOUNT, editor.getValue())
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
    @DisplayName("setAsText should failed because is not an entity type")
    public void testTextIsInvalid() {
        // WHEN
        final InvalidEntityTypeException exception = assertThrows(
                InvalidEntityTypeException.class,
                () -> editor.setAsText("aaa")
        );

        // THEN
        assertEquals("Value aaa is invalid, instead it should be one of"
                + " [HEADCOUNT, PRODUCTIVITY, THROUGHPUT, REMAINING_PROCESSING]",
                exception.getMessage());
    }
}
