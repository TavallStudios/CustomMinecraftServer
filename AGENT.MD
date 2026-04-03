# Repository Guidelines

- Runtime target: Java 25 with Netty. This repository is a standalone custom Minecraft server runtime, not a fork or plugin project.
- Scope discipline: keep the implementation limited to handshake, status, login proof-of-life, and early Bedrock connection flow. Do not add world, entity, command, or plugin systems.
- Architecture: prefer small focused classes, direct instancing, and explicit names. Avoid reflection-heavy registries, dependency injection frameworks, and speculative abstractions.
- Testing: prefer real protocol classes and data objects in tests. Mockito should be used for delegation verification or small seam isolation, not as a substitute for real packet/state objects.
- Observability: structured connection logs are a first-class deliverable. Unsupported states should fail fast with clear logs.
- MCP usage: local filesystem access is available through `/srv/local-pc-root/F:/workspace/CustomMinecraftServer` in the current Codex setup. Adjust this note if the local-drive mount changes.

Customize this file if the repository adds stricter build, branching, or review rules later.
