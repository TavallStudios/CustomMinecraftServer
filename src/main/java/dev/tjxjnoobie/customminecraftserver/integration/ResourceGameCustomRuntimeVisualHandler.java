package dev.tjxjnoobie.customminecraftserver.integration;

import org.tavall.api.minecraft.MinecraftVisualRenderRequest;

public final class ResourceGameCustomRuntimeVisualHandler {
    public String describeNoop(MinecraftVisualRenderRequest request) {
        return "CustomMinecraftServer visual request is a no-op until world join/chunks/entities exist: "
                + request.visualType() + " correlationId=" + request.correlationId();
    }
}
