package otus.krasnyanskysa.jwtapi.controller;

import otus.krasnyanskysa.jwtapi.dto.AuthResponse;
import otus.krasnyanskysa.jwtapi.dto.LoginRequest;
import otus.krasnyanskysa.jwtapi.dto.RegisterRequest;
import otus.krasnyanskysa.jwtapi.dto.TaskRequest;
import otus.krasnyanskysa.jwtapi.dto.TaskResponse;
import otus.krasnyanskysa.jwtapi.dto.UserResponse;
import otus.krasnyanskysa.jwtapi.entity.Role;
import otus.krasnyanskysa.jwtapi.entity.TaskStatus;
import otus.krasnyanskysa.jwtapi.repository.TaskRepository;
import otus.krasnyanskysa.jwtapi.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TaskIntegrationTest {

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

    private String loginAs(String username, String password) {
        rest.postForEntity(base() + "/api/auth/register",
                new RegisterRequest(username, username + "@test.com", password), AuthResponse.class);
        return rest.postForEntity(base() + "/api/auth/login",
                new LoginRequest(username, password), AuthResponse.class).getBody().token();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test
    void taskCrudFlow_asUser() {
        String token = loginAs("alice", "pass123!");

        // Create
        ResponseEntity<TaskResponse> created = rest.exchange(
                base() + "/api/tasks", HttpMethod.POST,
                new HttpEntity<>(new TaskRequest("Buy milk", "2 litres", TaskStatus.OPEN, null), bearerHeaders(token)),
                TaskResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        Long id = created.getBody().id();

        // Fetch
        ResponseEntity<TaskResponse> fetched = rest.exchange(
                base() + "/api/tasks/" + id, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)), TaskResponse.class);
        assertEquals("Buy milk", fetched.getBody().title());

        // Update
        ResponseEntity<TaskResponse> updated = rest.exchange(
                base() + "/api/tasks/" + id, HttpMethod.PUT,
                new HttpEntity<>(new TaskRequest("Buy milk", null, TaskStatus.DONE, null), bearerHeaders(token)),
                TaskResponse.class);
        assertEquals(TaskStatus.DONE, updated.getBody().status());

        // Delete
        ResponseEntity<Void> deleted = rest.exchange(
                base() + "/api/tasks/" + id, HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(token)), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
    }

    @Test
    void changeRole_viaApi_promotesUserToAdmin() {
        loginAs("alice", "pass123!");
        loginAs("bob", "pass123!");

        // Promote alice to admin via repository (to obtain an admin token)
        userRepository.findByUsername("alice").ifPresent(u -> {
            u.setRole(Role.ADMIN);
            userRepository.save(u);
        });
        String adminToken = rest.postForEntity(base() + "/api/auth/login",
                new LoginRequest("alice", "pass123!"), AuthResponse.class).getBody().token();

        Long bobId = userRepository.findByUsername("bob").orElseThrow().getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        ResponseEntity<UserResponse> result = rest.exchange(
                base() + "/api/users/" + bobId + "/role?role=ADMIN", HttpMethod.PUT,
                new HttpEntity<>(headers), UserResponse.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(Role.ADMIN, result.getBody().role());
        assertEquals("bob", result.getBody().username());
    }

    @Test
    void adminCanSeeAllTasks() {
        String aliceToken = loginAs("alice", "pass123!");
        rest.exchange(base() + "/api/tasks", HttpMethod.POST,
                new HttpEntity<>(new TaskRequest("Alice task", null, TaskStatus.OPEN, null), bearerHeaders(aliceToken)),
                TaskResponse.class);

        String bobToken = loginAs("bob", "pass123!");
        rest.exchange(base() + "/api/tasks", HttpMethod.POST,
                new HttpEntity<>(new TaskRequest("Bob task", null, TaskStatus.OPEN, null), bearerHeaders(bobToken)),
                TaskResponse.class);

        // bob becomes admin
        userRepository.findByUsername("bob").ifPresent(u -> {
            u.setRole(Role.ADMIN);
            userRepository.save(u);
        });
        String adminToken = rest.postForEntity(base() + "/api/auth/login",
                new LoginRequest("bob", "pass123!"), AuthResponse.class).getBody().token();

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> all = rest.exchange(
                base() + "/api/tasks", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(adminToken)), Map.class);
        assertEquals(HttpStatus.OK, all.getStatusCode());
        List<?> content = (List<?>) all.getBody().get("content");
        assertEquals(2, content.size());
    }
}
