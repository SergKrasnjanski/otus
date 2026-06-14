package otus.krasnyanskysa.jwtapi.security;

import otus.krasnyanskysa.jwtapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret",
                "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUpXVA==");
        ReflectionTestUtils.setField(provider, "expiration", 86400000L);
    }

    @Test
    void generateToken_extractUsername_success() {
        User user = user("alice");
        String token = provider.generateToken(user);
        assertNotNull(token);
        assertEquals("alice", provider.extractUsername(token));
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        User user = user("alice");
        assertTrue(provider.isTokenValid(provider.generateToken(user), user));
    }

    @Test
    void isTokenValid_wrongUser_returnsFalse() {
        User alice = user("alice");
        User bob = user("bob");
        assertFalse(provider.isTokenValid(provider.generateToken(alice), bob));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        JwtTokenProvider shortLived = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLived, "secret",
                "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUpXVA==");
        ReflectionTestUtils.setField(shortLived, "expiration", -1000L);

        User user = user("alice");
        assertFalse(shortLived.isTokenValid(shortLived.generateToken(user), user));
    }

    private User user(String username) {
        User u = new User();
        u.setUsername(username);
        return u;
    }
}

