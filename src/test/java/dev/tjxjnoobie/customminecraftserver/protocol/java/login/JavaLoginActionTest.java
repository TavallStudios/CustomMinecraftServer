package dev.tjxjnoobie.customminecraftserver.protocol.java.login;

import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaLoginActionTest {
    @Test
    void enumValuesAreStable() {
        TestLogSupport.logTestStart("JavaLoginActionTest.enumValuesAreStable");
        assertEquals(JavaLoginAction.DISCONNECT, JavaLoginAction.valueOf("DISCONNECT"));
        assertEquals(JavaLoginAction.REQUEST_ENCRYPTION, JavaLoginAction.valueOf("REQUEST_ENCRYPTION"));
    }
}
