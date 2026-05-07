package dev.tjxjnoobie.customminecraftserver.integration;

import com.tavall.hytale.resourcegame.shared.frontend.MinecraftVisualRenderRequest;

public final class ResourceGameCustomRuntimeVisualHandler {
    public String describeNoop(MinecraftVisualRenderRequest request) {
        return "CustomMinecraftServer visual request is a no-op until world join/chunks/entities exist: "
                + request.visualType() + " correlationId=" + request.correlationId();
    }
}
