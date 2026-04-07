package dev.tjxjnoobie.customminecraftserver.protocol.java.auth;

import dev.tjxjnoobie.customminecraftserver.config.AuthMode;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionEdition;
import dev.tjxjnoobie.customminecraftserver.session.ConnectionSession;
import dev.tjxjnoobie.customminecraftserver.test.TestLogSupport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JavaPlayerUuidResolverTest {
    @Test
    void resolvesAuthenticatedIdentityIfPresent() {
        TestLogSupport.logTestStart("JavaPlayerUuidResolverTest.resolvesAuthenticatedIdentityIfPresent");
        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.ONLINE);
        session.authenticatedIdentity("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        UUID resolved = JavaPlayerUuidResolver.resolve(session);

        assertEquals(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"), resolved);
    }

    @Test
    void generatesOfflineUuidAndPersistsIdentity() {
        TestLogSupport.logTestStart("JavaPlayerUuidResolverTest.generatesOfflineUuidAndPersistsIdentity");
        ConnectionSession session = new ConnectionSession("remote", ConnectionEdition.JAVA, AuthMode.OFFLINE);
        session.username("PlayerOne");

        UUID resolved = JavaPlayerUuidResolver.resolve(session);
        UUID expected = UUID.nameUUIDFromBytes("OfflinePlayer:PlayerOne".getBytes(StandardCharsets.UTF_8));

        assertEquals(expected, resolved);
        assertNotNull(session.authenticatedIdentity());
        assertEquals(expected.toString().replace("-", ""), session.authenticatedIdentity());
    }
}
