package com.customminecraftserver.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerSettingsLoader {
    private final ObjectMapper objectMapper;

    public ServerSettingsLoader() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ServerSettings load(Path path) throws IOException {
        if (Files.notExists(path)) {
            ServerSettings defaults = ServerSettings.defaults();
            Files.writeString(path, objectMapper.writeValueAsString(defaults));
            return defaults;
        }
        return objectMapper.readValue(Files.readString(path), ServerSettings.class);
    }
}
