package dev.tjxjnoobie.customminecraftserver.protocol.java.state;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JavaNextStateTest {
    @Test
    void resolvesFromId() {
        TestLogSupport.logTestStart("JavaNextStateTest.resolvesFromId");
        assertEquals(JavaNextState.STATUS, JavaNextState.fromId(1));
        assertEquals(JavaNextState.LOGIN, JavaNextState.fromId(2));
    }

    @Test
    void rejectsUnknownIds() {
        TestLogSupport.logTestStart("JavaNextStateTest.rejectsUnknownIds");
        assertThrows(IllegalArgumentException.class, () -> JavaNextState.fromId(99));
    }
}
