package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.JavaCipherDecoder;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.JavaCipherEncoder;
import dev.tjxjnoobie.customminecraftserver.protocol.java.codec.JavaPacketFrameDecoder;
import dev.tjxjnoobie.customminecraftserver.protocol.java.connection.JavaConnectionHandler;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaEncryptionResponsePacket;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaPacketIds;

import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaLoginDecision;
import dev.tjxjnoobie.customminecraftserver.protocol.java.login.JavaPostLoginCoordinator;
import dev.tjxjnoobie.customminecraftserver.protocol.java.packet.JavaPacketWriter;
import dev.tjxjnoobie.customminecraftserver.protocol.java.state.JavaChannelState;

import dev.tjxjnoobie.customminecraftserver.logging.StructuredConnectionLogger;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.session.SessionState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Map;

public final class JavaOnlineAuthenticationCoordinator {
    private final StructuredConnectionLogger logger;
    private final JavaSessionService sessionService;
    private final JavaPostLoginCoordinator postLoginCoordinator;

    public JavaOnlineAuthenticationCoordinator(
            StructuredConnectionLogger logger,
            JavaSessionService sessionService,
            JavaPostLoginCoordinator postLoginCoordinator
    ) {
        this.logger = logger;
        this.sessionService = sessionService;
        this.postLoginCoordinator = postLoginCoordinator;
    }

    public void requestEncryption(ChannelHandlerContext context, ConnectionSession session, JavaLoginDecision decision) {
        JavaEncryptionChallenge challenge = decision.encryptionChallenge();
        context.channel().attr(JavaChannelState.ENCRYPTION_CHALLENGE_KEY).set(challenge);
        logger.outboundPacket(session, decision.responseType(), Map.of(
                "serverId", challenge.serverId(),
                "publicKeyLength", challenge.publicKeyBytes().length,
                "verifyTokenLength", challenge.verifyToken().length
        ));
        transition(session, SessionState.JAVA_ENCRYPTION_NEGOTIATION, Map.of("reason", "ONLINE_ENCRYPTION_REQUIRED"));
        context.writeAndFlush(JavaPacketWriter.encryptionRequest(context.alloc(), session.protocolVersion(), challenge));
    }

    public void handleEncryptionResponse(ChannelHandlerContext context, ConnectionSession session, int packetId, ByteBuf frame) {
        if (packetId != JavaPacketIds.ENCRYPTION_RESPONSE) {
            failAndClose(context, session, "Unsupported Java encryption packet id " + packetId, Map.of("packetId", packetId));
            return;
        }

        JavaEncryptionChallenge challenge = context.channel().attr(JavaChannelState.ENCRYPTION_CHALLENGE_KEY).get();
        if (challenge == null) {
            failAndClose(context, session, "Received Java encryption response without a pending challenge", Map.of("packetId", packetId));
            return;
        }

        JavaEncryptionResponsePacket response = JavaEncryptionResponsePacket.read(frame);
        logger.inboundPacket(session, "ENCRYPTION_RESPONSE", Map.of(
                "packetId", packetId,
                "sharedSecretLength", response.sharedSecret().length,
                "verifyTokenLength", response.verifyToken().length
        ));

        byte[] sharedSecret;
        byte[] verifyToken;
        try {
            sharedSecret = decryptRsa(response.sharedSecret(), challenge.privateKey());
            verifyToken = decryptRsa(response.verifyToken(), challenge.privateKey());
        } catch (GeneralSecurityException exception) {
            sendDisconnect(context, session, "Java ONLINE auth failed: invalid encryption response", "LOGIN_DISCONNECT_AUTH_FAILURE", "ONLINE_AUTH_FAILED");
            return;
        }

        if (!Arrays.equals(challenge.verifyToken(), verifyToken)) {
            sendDisconnect(context, session, "Java ONLINE auth failed: verify token mismatch", "LOGIN_DISCONNECT_AUTH_FAILURE", "ONLINE_AUTH_FAILED");
            return;
        }

        try {
            enableEncryption(context, sharedSecret);
        } catch (GeneralSecurityException exception) {
            sendDisconnect(context, session, "Java ONLINE auth failed: unable to enable encryption", "LOGIN_DISCONNECT_AUTH_FAILURE", "ONLINE_AUTH_FAILED");
            return;
        }

        transition(session, SessionState.JAVA_AUTHENTICATING, Map.of("reason", "ENCRYPTION_ESTABLISHED"));
        JavaSessionVerificationRequest request = new JavaSessionVerificationRequest(
                session.username(),
                challenge.serverId(),
                sharedSecret,
                challenge.publicKeyBytes(),
                session.remoteAddress()
        );
        sessionService.verifyJoin(request).whenComplete((result, error) ->
                context.executor().execute(() -> finishOnlineVerification(context, session, result, error))
        );
    }

    private void finishOnlineVerification(
            ChannelHandlerContext context,
            ConnectionSession session,
            JavaSessionVerificationResult result,
            Throwable error
    ) {
        if (!context.channel().isActive()) {
            return;
        }
        context.channel().attr(JavaChannelState.ENCRYPTION_CHALLENGE_KEY).set(null);

        if (error != null) {
            logger.warning(session, "java_online_auth_failed", Map.of("message", rootMessage(error)));
            sendDisconnect(
                    context,
                    session,
                    "Java ONLINE auth failed: " + rootMessage(error),
                    "LOGIN_DISCONNECT_AUTH_FAILURE",
                    "ONLINE_AUTH_FAILED"
            );
            return;
        }

        session.authenticatedIdentity(result.profileId());
        session.username(result.profileName());
        String protocolFamily = session.protocolFamily() == null ? "JAVA_UNKNOWN" : session.protocolFamily();
        JavaChannelState.terminalMessage(
                context,
                "Custom server auth reached successfully [" + protocolFamily + " ONLINE profile=" + result.profileName() + "]"
        );
        postLoginCoordinator.beginPostLoginFlow(context, session, "ONLINE_AUTH_VERIFIED");
    }

    private void sendDisconnect(
            ChannelHandlerContext context,
            ConnectionSession session,
            String message,
            String responseType,
            String transitionReason
    ) {
        logger.outboundPacket(session, responseType, Map.of("disconnectMessage", message));
        transition(session, SessionState.JAVA_TERMINATED, Map.of("reason", transitionReason));
        context.writeAndFlush(JavaPacketWriter.disconnect(context.alloc(), message)).addListener(ChannelFutureListener.CLOSE);
    }

    private void enableEncryption(ChannelHandlerContext context, byte[] sharedSecret) throws GeneralSecurityException {
        Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sharedSecret, "AES"), new IvParameterSpec(sharedSecret));

        Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sharedSecret, "AES"), new IvParameterSpec(sharedSecret));

        ChannelPipeline pipeline = context.pipeline();
        String frameDecoderName = pipeline.context(JavaPacketFrameDecoder.class) != null
                ? pipeline.context(JavaPacketFrameDecoder.class).name()
                : "java-frame-decoder";
        String handlerName = pipeline.context(JavaConnectionHandler.class) != null
                ? pipeline.context(JavaConnectionHandler.class).name()
                : "java-connection-handler";
        if (pipeline.get("java-decrypt") == null) {
            pipeline.addBefore(frameDecoderName, "java-decrypt", new JavaCipherDecoder(decryptCipher));
        }
        if (pipeline.get("java-encrypt") == null) {
            pipeline.addBefore(handlerName, "java-encrypt", new JavaCipherEncoder(encryptCipher));
        }
    }

    private byte[] decryptRsa(byte[] encrypted, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encrypted);
    }

    private void transition(ConnectionSession session, SessionState nextState, Map<String, ?> details) {
        SessionState previous = session.state();
        session.state(nextState);
        logger.transition(session, previous, nextState, details);
    }

    private void failAndClose(ChannelHandlerContext context, ConnectionSession session, String message, Map<String, ?> details) {
        logger.warning(session, "java_protocol_failure", Map.of(
                "packetId", details.get("packetId"),
                "message", message
        ));
        context.close();
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}



