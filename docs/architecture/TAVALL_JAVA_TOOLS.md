# CustomMinecraftServer Tavall Java Tools Contract

CustomMinecraftServer is a Tavall-owned Java consumer. Tavall DI is the universal first-party composition/lifecycle baseline.

The server runtime uses Tavall Concurrency for asynchronous/shared coordination and Tavall Logging for application/runtime diagnostics. Existing SLF4J/Logback wiring is a temporary compatibility seam while source calls migrate.

Use Tavall Registry, EventBus, Cache, Database, Reflection, and Scheduler whenever those concerns are introduced rather than creating server-local equivalents.

Netty remains the networking/protocol framework. Tavall tools do not replace Netty's channel/event-loop mechanics; they own Tavall application infrastructure above that external boundary.

Do not add first-party ServiceLoader composition, hand-built dependency containers, custom executor frameworks, logging facades, registry/cache/event-bus frameworks, reflection scanners, scheduled executors, or database infrastructure when a Tavall tool owns the concern.

Exact Java 25 build/tests and staged server acceptance remain required before promotion.