package dev.tjxjnoobie.customminecraftserver.bootstrap;

import java.util.List;
import java.util.Locale;

final class ServerConsoleCommandProcessor {
    private final ServerConsoleContext context;

    ServerConsoleCommandProcessor(ServerConsoleContext context) {
        this.context = context;
    }

    ServerConsoleCommandResult handle(String rawInput) {
        String command = rawInput == null ? "" : rawInput.trim();
        if (command.isEmpty()) {
            return ServerConsoleCommandResult.continueRunning("");
        }

        return switch (command.toLowerCase(Locale.ROOT)) {
            case "help", "?" -> ServerConsoleCommandResult.continueRunning(
                    "Commands: help, status, sessions, stop"
            );
            case "status" -> ServerConsoleCommandResult.continueRunning(formatStatus(context.snapshot()));
            case "sessions" -> ServerConsoleCommandResult.continueRunning(formatSessions(context.activeSessionSummaryLines()));
            case "stop", "exit", "quit" -> {
                context.requestStop();
                yield ServerConsoleCommandResult.stop("Stopping server...");
            }
            default -> ServerConsoleCommandResult.continueRunning(
                    "Unknown command: " + command + ". Commands: help, status, sessions, stop"
            );
        };
    }

    private String formatStatus(ServerConsoleSnapshot snapshot) {
        return "status"
                + " running=" + snapshot.running()
                + " host=" + snapshot.host()
                + " javaTcpPort=" + snapshot.javaTcpPort()
                + " bedrockUdpPort=" + snapshot.bedrockUdpPort()
                + " authMode=" + snapshot.authMode()
                + " activeSessions=" + snapshot.activeSessions();
    }

    private String formatSessions(List<String> sessionSummaryLines) {
        if (sessionSummaryLines.isEmpty()) {
            return "No active sessions.";
        }
        return String.join(System.lineSeparator(), sessionSummaryLines);
    }
}

