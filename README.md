# Custom Minecraft Server MVP

Standalone Minecraft server runtime written in Java 25 with Netty. This is not a Paper fork, plugin host, or compatibility layer. The codebase exists to prove that our own networking, session, and protocol pipeline can accept real Java Edition and Bedrock clients, identify their handshake or login path, and return deterministic protocol-aware responses without any runtime dependency on Bukkit, Spigot, Paper, Velocity, Fabric, Forge, Minestom, or Mojang server jars.

## MVP target

- Java Edition `1.8.x`
- Java Edition `1.21.x`
- Bedrock latest stable family validated here as `26.10` / protocol `944`

Success for this MVP means:

- the client connects
- the server identifies edition and protocol family or version
- the server decodes the first meaningful handshake or login packet path
- the server sends a valid edition-aware response
- the client reacts to our custom response instead of timing out

World join, chunks, entities, commands, and gameplay are intentionally out of scope.

## Project layout

- `src/main/java/com/customminecraftserver/bootstrap`: server entry point and Netty bootstraps
- `src/main/java/com/customminecraftserver/configuration`: strongly typed `server-settings.json` model
- `src/main/java/com/customminecraftserver/logging`: structured connection logging
- `src/main/java/com/customminecraftserver/session`: connection and session state model
- `src/main/java/com/customminecraftserver/networking`: protocol version detection
- `src/main/java/com/customminecraftserver/javaedition`: Java framing, login, encryption, and session verification
- `src/main/java/com/customminecraftserver/bedrock`: RakNet, Bedrock login parsing, and Bedrock auth admission
- `src/test/java/com/customminecraftserver/integration`: JUnit and Mockito integration tests using real handlers, packets, and session objects
- `harness`: Mineflayer and `bedrock-protocol` smoke harness
- `scripts`: local Windows PowerShell entry points for server boot and smoke tests

## Configuration

`server-settings.json`

```json
{
  "host": "0.0.0.0",
  "javaTcpPort": 25565,
  "bedrockUdpPort": 19132,
  "motd": "Custom server handshake reached successfully",
  "maxConnections": 128,
  "structuredLoggingEnabled": true,
  "authMode": "OFFLINE",
  "javaAuthentication": {
    "sessionServerUrl": "https://sessionserver.mojang.com",
    "includeClientIpInSessionVerification": false,
    "rsaKeySizeBits": 1024
  },
  "bedrockAuthentication": {
    "requireTrustedRootChain": true,
    "trustedRootPublicKeys": [
      "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAECRXueJeTDqNRRgJi/vlRufByu/2G0i2Ebt6YMar5QX/R0DIIyrJMcUpruK4QveTfJSTp3Shlq4Gk34cD/4GUWwkv0DVuzeuB+tXija7HBxii03NHDbPAD0AKnLr2wdAp"
    ]
  }
}
```

Current auth behavior:

- `OFFLINE`: fully supported for the handshake MVP. Java and Bedrock proceed far enough to prove decode, state routing, post-login branching, and deterministic terminal packets.
- `ONLINE` Java: implemented. The server sends a real encryption request, decrypts the client's response, enables AES packet encryption, calls Mojang session verification, sends `Login Success`, completes the modern configuration handoff, performs one play keepalive roundtrip, waits for a client settings packet or timeout, and then sends a deterministic disconnect.
- `ONLINE` Java: implemented. The server sends a real encryption request, decrypts the client's response, enables AES packet encryption, calls Mojang session verification, sends `Login Success`, completes the modern configuration handoff, performs one play keepalive roundtrip, waits for client settings, performs one clientbound `ping` / serverbound `pong` roundtrip, and then sends a deterministic disconnect.
- `ONLINE` Bedrock: implemented through the first encrypted initialization bootstrap. The server verifies the incoming identity chain against trusted root keys, completes the secure-session handshake, emits encrypted `play_status login_success`, completes resource-pack negotiation, answers `request_chunk_radius` with `chunk_radius_update`, emits a minimal encrypted `start_game` plus `play_status player_spawn`, and then sends a deterministic authenticated disconnect after either local-player initialization or a short deterministic timeout.

## Supported packet and state paths

Java Edition:

- Handshake `0x00`
- Status Request `0x00`
- Status Ping `0x01`
- Login Start `0x00`
- Encryption Request `0x01` clientbound
- Encryption Response `0x01` serverbound
- Login Success `0x02` clientbound
- Login Acknowledged `0x03` serverbound
- Finish Configuration `0x03` clientbound
- Finish Configuration `0x03` serverbound
- Play Keep Alive `0x27` clientbound
- Play Keep Alive `0x1a` serverbound
- Settings `0x0c` serverbound
- Play Ping `0x37` clientbound
- Play Pong `0x2b` serverbound
- Legacy Play Disconnect `0x40` clientbound
- Modern Play Disconnect `0x1d` clientbound

Java state flow:

- `JAVA_HANDSHAKE -> JAVA_STATUS`
- `JAVA_HANDSHAKE -> JAVA_LOGIN -> JAVA_TERMINATED`
- `JAVA_HANDSHAKE -> JAVA_LOGIN -> JAVA_WAITING_LOGIN_ACK -> JAVA_CONFIGURING -> JAVA_PLAY -> JAVA_WAITING_SETTINGS -> JAVA_WAITING_PONG -> JAVA_TERMINATED`
- `JAVA_HANDSHAKE -> JAVA_LOGIN -> JAVA_ENCRYPTION_NEGOTIATION -> JAVA_AUTHENTICATING -> JAVA_TERMINATED`
- `JAVA_HANDSHAKE -> JAVA_LOGIN -> JAVA_ENCRYPTION_NEGOTIATION -> JAVA_AUTHENTICATING -> JAVA_WAITING_LOGIN_ACK -> JAVA_CONFIGURING -> JAVA_PLAY -> JAVA_WAITING_SETTINGS -> JAVA_WAITING_PONG -> JAVA_TERMINATED`

Bedrock / RakNet:

- Unconnected Ping / Pong
- Open Connection Request 1 / Reply 1
- Open Connection Request 2 / Reply 2
- Connection Request / Connection Request Accepted
- New Incoming Connection
- Connected Ping / Pong
- Bedrock `request_network_settings`
- Bedrock `network_settings`
- Bedrock `login`
- Bedrock `server_to_client_handshake`
- Bedrock `client_to_server_handshake`
- Bedrock `play_status login_success`
- Bedrock `resource_packs_info`
- Bedrock `resource_pack_client_response`
- Bedrock `resource_pack_stack`
- Bedrock `request_chunk_radius`
- Bedrock `chunk_radius_update`
- Bedrock `start_game`
- Bedrock `play_status player_spawn`
- Bedrock `set_local_player_as_initialized`
- Bedrock `disconnect`

Bedrock state flow:

- `BEDROCK_UNCONNECTED -> BEDROCK_OPEN_CONNECTION -> BEDROCK_CONNECTED -> BEDROCK_LOGIN`
- `BEDROCK_UNCONNECTED -> BEDROCK_OPEN_CONNECTION -> BEDROCK_CONNECTED -> BEDROCK_LOGIN -> BEDROCK_AUTHENTICATING`
- `BEDROCK_UNCONNECTED -> BEDROCK_OPEN_CONNECTION -> BEDROCK_CONNECTED -> BEDROCK_LOGIN -> BEDROCK_SECURE_SESSION -> BEDROCK_INITIALIZING -> BEDROCK_WAITING_CHUNK_RADIUS -> BEDROCK_WAITING_LOCAL_PLAYER_INITIALIZATION -> BEDROCK_CONNECTED`
- `BEDROCK_UNCONNECTED -> BEDROCK_OPEN_CONNECTION -> BEDROCK_CONNECTED -> BEDROCK_LOGIN -> BEDROCK_AUTHENTICATING -> BEDROCK_SECURE_SESSION -> BEDROCK_INITIALIZING -> BEDROCK_WAITING_CHUNK_RADIUS -> BEDROCK_WAITING_LOCAL_PLAYER_INITIALIZATION -> BEDROCK_CONNECTED`

## How to run

Requirements:

- Java `25`
- Node.js `22+`

Install the Node harness dependencies once:

```powershell
cd .\harness
npm install
```

Run the Java test suite:

```powershell
mvn test
```

Start the server manually:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-server.ps1
```

Publish a standalone server home under `Documents\CustomMCServer`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\publish-server-home.ps1
```

Server console commands after startup:

- `help`
- `status`
- `sessions`
- `stop`

Run the repeatable offline smoke suite:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-java-1.8.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\test-java-1.21.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\test-bedrock.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\test-all.ps1
```

Run the optional authenticated smoke scripts:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-java-online-1.8.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\test-java-online-1.21.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\test-bedrock-online.ps1
```

Authenticated harness notes:

- the Node harness uses Mineflayer and `bedrock-protocol`
- Microsoft device-code auth or cached Prismarine auth is used
- launcher session tokens are not scraped or reused
- the authenticated scripts are manual on purpose because they may prompt for device-code sign-in

## Automated harness

Offline Java harness:

- implemented with Mineflayer
- validates `1.8.8` and `1.21.4`
- asserts the disconnect reason contains:
  - `JAVA_1_8_X OFFLINE`
  - `JAVA_1_21_X OFFLINE`
- exercises the post-login branch:
  - `1.8.8`: `Login Success` then legacy play disconnect
  - `1.21.4`: `Login Success`, client `login_acknowledged`, `finish_configuration`, one play keepalive roundtrip, client settings, one `ping` / `pong` roundtrip, then deterministic disconnect

Offline Bedrock harness:

- implemented with PrismarineJS `bedrock-protocol`
- validated against `26.10` / protocol `944`
- asserts the disconnect reason contains:
  - `BEDROCK protocol=944 OFFLINE`
- proves the secure-session exchange:
  - unencrypted `server_to_client_handshake`
  - encrypted `client_to_server_handshake`
  - encrypted `play_status login_success`
  - encrypted `resource_packs_info`
  - encrypted `resource_pack_stack`
  - encrypted `chunk_radius_update`
  - encrypted `start_game`
  - encrypted disconnect

Optional authenticated harness:

- Java uses Mineflayer with `auth: 'microsoft'`
- Bedrock uses `bedrock-protocol` with `offline: false`
- both support Prismarine auth cache directories under `harness/auth-cache`
- both print Microsoft device-code instructions when cached auth is not already present

JUnit and Mockito integration coverage:

- real Java handshake and offline login path through Netty `EmbeddedChannel`
- real Java online login path with RSA challenge, AES encryption, and a local stub Mojang session service
- explicit modern Java settings-packet coverage after the first play keepalive roundtrip
- explicit modern Java `ping` / `pong` coverage after the settings boundary
- delegation-style auth routing tests using real admissions and Mockito spies only for verification
- real Bedrock RakNet, secure-session, and offline login path through Netty `EmbeddedChannel`
- real Bedrock online chain verification and secure-session path using generated ES384 key chains and a trusted-root test configuration
- explicit Bedrock `set_local_player_as_initialized` coverage after `start_game` and `player_spawn`

## Example automated output

From `powershell -ExecutionPolicy Bypass -File .\scripts\test-all.ps1`:

```text
edition=JAVA version=1.8.8 connected=true branchSelected=true sessionSignal=LEGACY_LOGIN_DISCONNECT sessionSignalObserved=true responseReceived=true reason={"text":"Custom server handshake reached successfully [JAVA_1_8_X OFFLINE]"}
edition=JAVA version=1.21.4 connected=true branchSelected=true sessionSignal=SETTINGS_PING_PONG sessionSignalObserved=true responseReceived=true reason={"type":"compound","value":{"text":{"type":"string","value":"Custom server handshake reached successfully [JAVA_1_21_X OFFLINE]"}}}
Connecting to 127.0.0.1:19132 Custom server handshake reached successfully (CustomMvp), version 26.10
Server requested disconnect: Custom server handshake reached successfully [BEDROCK protocol=944 OFFLINE]
edition=BEDROCK version=26.10 connected=true branchSelected=true sessionSignal=START_GAME_BOOTSTRAP sessionSignalObserved=true responseReceived=true reason=Custom server handshake reached successfully [BEDROCK protocol=944 OFFLINE]
```

## Example connection logs

Java `1.8.8` offline:

```text
event=packet_inbound edition=JAVA protocolVersion=47 protocolFamily=JAVA_1_8_X packet=HANDSHAKE nextState=LOGIN
event=packet_inbound edition=JAVA protocolVersion=47 protocolFamily=JAVA_1_8_X username=LegacyHarness packet=LOGIN_START
event=packet_outbound edition=JAVA protocolVersion=47 protocolFamily=JAVA_1_8_X username=LegacyHarness authenticatedIdentity=5ed3c6ccbb063b49a92cce60fae1c6de packet=LOGIN_SUCCESS
event=packet_outbound edition=JAVA protocolVersion=47 protocolFamily=JAVA_1_8_X packet=PLAY_DISCONNECT disconnectMessage="Custom server handshake reached successfully [JAVA_1_8_X OFFLINE]"
```

Java `1.21.4` online:

```text
event=packet_inbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X username=ModernOnlineUser packet=LOGIN_START
event=packet_outbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X packet=ENCRYPTION_REQUEST serverId=3a9cf2f1
event=state_transition edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X fromState=JAVA_ENCRYPTION_NEGOTIATION toState=JAVA_AUTHENTICATING reason=ENCRYPTION_ESTABLISHED
event=packet_outbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X username=VerifiedModern authenticatedIdentity=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb packet=LOGIN_SUCCESS
event=packet_inbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X packet=LOGIN_ACKNOWLEDGED
event=packet_outbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X packet=FINISH_CONFIGURATION
event=packet_outbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X packet=PLAY_KEEP_ALIVE keepAliveId=1161981756646125696
event=packet_inbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X packet=SETTINGS locale=en_US viewDistance=12
event=packet_outbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X packet=PLAY_PING pingId=270544960
event=packet_inbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X packet=PLAY_PONG pingId=270544960
event=packet_outbound edition=JAVA protocolVersion=769 protocolFamily=JAVA_1_21_X packet=PLAY_DISCONNECT disconnectMessage="Custom server auth reached successfully [JAVA_1_21_X ONLINE profile=VerifiedModern]"
```

Bedrock `26.10` offline:

```text
event=packet_inbound edition=BEDROCK bedrockStage=REQUEST_NETWORK_SETTINGS clientProtocol=944 packet=REQUEST_NETWORK_SETTINGS
event=packet_outbound edition=BEDROCK protocolVersion=944 packet=NETWORK_SETTINGS responseType=NETWORK_SETTINGS
event=packet_inbound edition=BEDROCK protocolVersion=944 username=BedrockHarness bedrockStage=LOGIN packet=LOGIN
event=packet_outbound edition=BEDROCK protocolVersion=944 packet=SERVER_TO_CLIENT_HANDSHAKE
event=packet_inbound edition=BEDROCK protocolVersion=944 username=BedrockHarness bedrockStage=CLIENT_TO_SERVER_HANDSHAKE packet=CLIENT_TO_SERVER_HANDSHAKE
event=packet_outbound edition=BEDROCK protocolVersion=944 packet=PLAY_STATUS status=login_success
event=packet_outbound edition=BEDROCK protocolVersion=944 packet=RESOURCE_PACKS_INFO
event=packet_inbound edition=BEDROCK protocolVersion=944 username=BedrockHarness packet=RESOURCE_PACK_CLIENT_RESPONSE responseStatus=completed responseNumber=1
event=packet_outbound edition=BEDROCK protocolVersion=944 packet=RESOURCE_PACK_STACK gameVersion=26.10
event=packet_inbound edition=BEDROCK protocolVersion=944 username=BedrockHarness packet=REQUEST_CHUNK_RADIUS requestedChunkRadius=10
event=packet_outbound edition=BEDROCK protocolVersion=944 packet=CHUNK_RADIUS_UPDATE chunkRadius=10
event=packet_outbound edition=BEDROCK protocolVersion=944 packet=START_GAME runtimeEntityId=1 gameVersion=26.10
event=packet_outbound edition=BEDROCK protocolVersion=944 packet=PLAY_STATUS status=player_spawn
event=packet_outbound edition=BEDROCK protocolVersion=944 packet=DISCONNECT disconnectMessage="Custom server handshake reached successfully [BEDROCK protocol=944 OFFLINE]"
```

Bedrock `26.10` online:

```text
event=packet_inbound edition=BEDROCK protocolVersion=944 username=VerifiedBedrock authenticatedIdentity=bedrock-player-identity authenticatedXuid=2535400000000001 bedrockStage=LOGIN packet=LOGIN
event=state_transition edition=BEDROCK protocolVersion=944 fromState=BEDROCK_LOGIN toState=BEDROCK_AUTHENTICATING reason=ONLINE_AUTH_VERIFIED
event=state_transition edition=BEDROCK protocolVersion=944 fromState=BEDROCK_AUTHENTICATING toState=BEDROCK_SECURE_SESSION reason=SECURE_SESSION_REQUIRED
event=packet_outbound edition=BEDROCK protocolVersion=944 username=VerifiedBedrock authenticatedXuid=2535400000000001 packet=SERVER_TO_CLIENT_HANDSHAKE
event=packet_outbound edition=BEDROCK protocolVersion=944 username=VerifiedBedrock authenticatedXuid=2535400000000001 packet=PLAY_STATUS status=login_success
event=packet_outbound edition=BEDROCK protocolVersion=944 username=VerifiedBedrock authenticatedXuid=2535400000000001 packet=RESOURCE_PACKS_INFO
event=packet_outbound edition=BEDROCK protocolVersion=944 username=VerifiedBedrock authenticatedXuid=2535400000000001 packet=RESOURCE_PACK_STACK gameVersion=26.10
event=packet_outbound edition=BEDROCK protocolVersion=944 username=VerifiedBedrock authenticatedXuid=2535400000000001 packet=CHUNK_RADIUS_UPDATE chunkRadius=10
event=packet_outbound edition=BEDROCK protocolVersion=944 username=VerifiedBedrock authenticatedXuid=2535400000000001 packet=START_GAME runtimeEntityId=1 gameVersion=26.10
event=packet_outbound edition=BEDROCK protocolVersion=944 username=VerifiedBedrock authenticatedXuid=2535400000000001 packet=PLAY_STATUS status=player_spawn
event=packet_outbound edition=BEDROCK protocolVersion=944 username=VerifiedBedrock authenticatedXuid=2535400000000001 packet=DISCONNECT disconnectMessage="Custom server auth reached successfully [BEDROCK protocol=944 ONLINE xuid=2535400000000001]"
```

## Known limitations

- Java stops intentionally after a minimal configuration handoff, one play keepalive roundtrip, client settings, and one `ping` / `pong` roundtrip.
- Java compression remains out of scope.
- Bedrock stops intentionally after resource-pack negotiation, one `chunk_radius_update`, a minimal `start_game` bootstrap, and a deterministic disconnect. The server-side `set_local_player_as_initialized` path is implemented and covered by integration tests, but the live Prismarine smoke currently proves `start_game` rather than the full local-player-init roundtrip.
- Bedrock reliability handling is intentionally minimal: it is sufficient for the handshake proof path, not a complete RakNet implementation.
- The optional authenticated smoke scripts require a real Microsoft sign-in via device code or cached Prismarine auth and were not executed automatically in this environment.

## Auth mode behavior

Startup always logs the configured auth mode. Each connection log carries `authMode`.

- `OFFLINE` mode logs an explicit offline-mode startup warning.
- `ONLINE` mode logs that Java session verification and Bedrock secure-session handshakes are active, while later session bootstrap remains out of scope.
- Java online failures produce deterministic login-state disconnects instead of silent socket closes.
- Bedrock online failures produce deterministic disconnects instead of pretending to trust an unverified chain.

More detail is documented in [docs/auth-research-notes.md](./docs/auth-research-notes.md).

## Next MVP

Implement the first stable initialized-session bootstrap after the current proof boundary:

- Java: emit the first minimal real-client bootstrap packet after the current `ping` / `pong` proof point so a vanilla-compatible client can stay alive without an immediate disconnect
- Bedrock: complete enough post-`start_game` initialization for the real client smoke to emit `set_local_player_as_initialized` without relying on the integration-only synthetic packet
