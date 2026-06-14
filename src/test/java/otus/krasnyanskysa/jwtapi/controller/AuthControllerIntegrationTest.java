package otus.krasnyanskysa.jwtapi.controller;

import otus.krasnyanskysa.jwtapi.dto.AuthResponse;
import otus.krasnyanskysa.jwtapi.dto.LoginRequest;
import otus.krasnyanskysa.jwtapi.dto.RegisterRequest;
import otus.krasnyanskysa.jwtapi.repository.TaskRepository;
import otus.krasnyanskysa.jwtapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort int port;
    RestTemplate rest = new RestTemplate();

    @Autowired UserRepository userRepository;
    @Autowired TaskRepository taskRepository;

    @BeforeEach
    void clean() {
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String base() { return "http://localhost:" + port; }

    @Test
    void fullAuthFlow_registerLoginAccessProtected() {
        // Register
        var reg = new RegisterRequest("alice", "alice@test.com", "password123");
        ResponseEntity<AuthResponse> regResp = rest.postForEntity(base() + "/api/auth/register", reg, AuthResponse.class);
        assertEquals(HttpStatus.CREATED, regResp.getStatusCode());
        assertNotNull(regResp.getBody().token());

        // Login
        ResponseEntity<AuthResponse> loginResp = rest.postForEntity(base() + "/api/auth/login",
                new LoginRequest("alice", "password123"), AuthResponse.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        String token = loginResp.getBody().token();
        assertFalse(token.isBlank());

        // Access protected endpoint with token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> tasksResp = rest.exchange(
                base() + "/api/tasks", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, tasksResp.getStatusCode());
    }

    @Test
    void accessProtected_withoutToken_returns401() {
        try {
            rest.getForEntity(base() + "/api/tasks", String.class);
            fail("Expected 401");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    void register_duplicateUsername_returns400() {
        rest.postForEntity(base() + "/api/auth/register",
                new RegisterRequest("bob", "bob@test.com", "password123"), AuthResponse.class);
        try {
            rest.postForEntity(base() + "/api/auth/register",
                    new RegisterRequest("bob", "bob2@test.com", "password123"), String.class);
            fail("Expected 400");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        }
    }

    @Test
    void login_wrongPassword_returns401() {
        rest.postForEntity(base() + "/api/auth/register",
                new RegisterRequest("carol", "carol@test.com", "password123"), AuthResponse.class);
        try {
            rest.postForEntity(base() + "/api/auth/login",
                    new LoginRequest("carol", "wrongpass"), String.class);
            fail("Expected 401");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    void getUsers_asUser_returns403() {
        rest.postForEntity(base() + "/api/auth/register",
                new RegisterRequest("dave", "dave@test.com", "password123"), AuthResponse.class);
        String token = rest.postForEntity(base() + "/api/auth/login",
                new LoginRequest("dave", "password123"), AuthResponse.class).getBody().token();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        try {
            rest.exchange(base() + "/api/users", HttpMethod.GET, new HttpEntity<>(headers), String.class);
            fail("Expected 403");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
    }
}
