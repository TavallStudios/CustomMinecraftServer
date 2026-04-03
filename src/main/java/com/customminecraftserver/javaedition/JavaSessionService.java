package com.customminecraftserver.javaedition;

import java.util.concurrent.CompletableFuture;

public interface JavaSessionService {
    CompletableFuture<JavaSessionVerificationResult> verifyJoin(JavaSessionVerificationRequest request);
}
