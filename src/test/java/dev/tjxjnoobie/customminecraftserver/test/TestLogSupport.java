package dev.tjxjnoobie.customminecraftserver.test;

public final class TestLogSupport {
    private TestLogSupport() {
    }

    public static void logTestStart(String testName) {
        System.out.println("[TEST] " + testName);
    }
}
