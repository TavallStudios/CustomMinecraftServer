package com.customminecraftserver.bootstrap;

record ServerConsoleCommandResult(String message, boolean stopRequested) {
    static ServerConsoleCommandResult continueRunning(String message) {
        return new ServerConsoleCommandResult(message, false);
    }

    static ServerConsoleCommandResult stop(String message) {
        return new ServerConsoleCommandResult(message, true);
    }
}
