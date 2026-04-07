package dev.tjxjnoobie.customminecraftserver.bootstrap;

import java.util.List;

interface ServerConsoleContext {
    ServerConsoleSnapshot snapshot();

    List<String> activeSessionSummaryLines();

    void requestStop();
}

