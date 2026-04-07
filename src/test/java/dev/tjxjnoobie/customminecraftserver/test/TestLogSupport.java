package dev.tjxjnoobie.customminecraftserver.test;

import com.tjxjnoobie.api.platform.global.console.Log;

public final class TestLogSupport {
    private TestLogSupport() {
    }

    public static void logTestStart(String testName) {
        Log.info("[TEST] " + testName);
    }
}
