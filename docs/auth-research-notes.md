# Authentication Research Notes

This repository now has a real auth boundary instead of only a placeholder switch. The goal remains narrow: prove that our own runtime can negotiate the earliest meaningful authenticated paths without copying Paper, Mojang server runtime architecture, or Bedrock libraries 1:1.

## Java Edition

Implemented path in this repository:

1. Handshake packet carries protocol version, host, port, and next state.
2. `OFFLINE` mode now reaches the first protocol-correct post-login boundary:
   - Java `1.8.x`: `Login Success` then legacy play disconnect
   - Java `1.21.x`: `Login Success`, client `login_acknowledged`, `finish_configuration`, one play keepalive roundtrip, client settings, one `ping` / `pong` roundtrip, then play disconnect
3. `ONLINE` mode now:
   - accepts `Login Start`
   - sends `Encryption Request`
   - parses `Encryption Response`
   - decrypts the shared secret and verify token with the server RSA private key
   - enables AES packet encryption
   - calls Mojang session verification with the computed server hash
   - sends `Login Success`
   - on modern clients, waits for `login_acknowledged`
   - completes the modern configuration handoff
   - performs one play keepalive roundtrip
   - waits for a client settings packet
   - performs one deterministic `ping` / `pong` roundtrip
   - sends the first controlled terminal packet for the target protocol family

Reference implementation areas studied:

- Prismarine `minecraft-protocol`
  - `src/server/login.js`
  - `src/client/encrypt.js`
- Prismarine `yggdrasil`
  - `src/Server.js`

Important Java auth boundaries in this repository:

- `JavaLoginCoordinator`
- `OnlineJavaLoginAdmission`
- `JavaEncryptionChallengeFactory`
- `MojangJavaSessionService`
- `JavaCipherDecoder`
- `JavaCipherEncoder`

What is still intentionally not implemented on Java:

- remaining configuration packets beyond the MVP handoff
- compression for authenticated sessions
- extended play-state join or gameplay

## Bedrock

Implemented path in this repository:

1. RakNet unconnected ping or pong
2. Open connection request or reply
3. RakNet connection request or accept
4. New incoming connection
5. Bedrock `request_network_settings`
6. Bedrock `network_settings`
7. Bedrock `login`
8. `OFFLINE` mode now performs the same secure-session handshake shape as `ONLINE` once the client login packet exposes a usable handshake public key
9. `ONLINE` mode now:
   - parses the Bedrock identity chain from `chain` or `Certificate`
   - verifies the chain against configured trusted root public keys
   - verifies the final client JWT with the chain-derived identity key
   - extracts display name, identity, XUID, and the client handshake public key
   - sends `server_to_client_handshake`
   - accepts encrypted `client_to_server_handshake`
   - emits encrypted `play_status login_success`
   - performs the empty resource-pack negotiation
   - waits for the client's `request_chunk_radius`
   - responds with encrypted `chunk_radius_update`
   - emits a minimal encrypted `start_game`
   - emits encrypted `play_status player_spawn`
   - accepts `set_local_player_as_initialized` when the client sends it
   - sends a deterministic authenticated disconnect or a deterministic timeout disconnect if the real client stops at the minimal bootstrap boundary

Reference implementation areas studied:

- Prismarine `bedrock-protocol`
  - `src/handshake/loginVerify.js`
  - `src/handshake/login.js`
  - `src/handshake/keyExchange.js`
  - `src/serverPlayer.js`
  - `src/client/auth.js`

Important Bedrock auth boundaries in this repository:

- `BedrockLoginCoordinator`
- `OfflineBedrockLoginAdmission`
- `OnlineBedrockLoginAdmission`
- `BedrockJwtVerifier`

What is still intentionally not implemented on Bedrock:

- world or gameplay packets
- chunk or gameplay sync after the first `start_game` bootstrap

## Why the boundary still stops here

The current value checkpoint is proving:

- our own framing and state routing work
- Java can perform encrypted Mojang session verification without external server jars
- Bedrock can reject or accept login identity chains based on our own verification logic
- the runtime remains small, explicit, and debuggable

The next milestone is not gameplay. It is the first minimal initialized session:

- Java the first real client-stabilising bootstrap packet after the current `ping` / `pong` proof point
- Bedrock enough post-`start_game` bootstrap for the live client to emit `set_local_player_as_initialized` without the integration-only synthetic packet
