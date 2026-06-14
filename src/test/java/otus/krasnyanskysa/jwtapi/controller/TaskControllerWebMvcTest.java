package otus.krasnyanskysa.jwtapi.controller;

import otus.krasnyanskysa.jwtapi.dto.TaskRequest;
import otus.krasnyanskysa.jwtapi.dto.TaskResponse;
import otus.krasnyanskysa.jwtapi.dto.UserResponse;
import otus.krasnyanskysa.jwtapi.entity.Role;
import otus.krasnyanskysa.jwtapi.entity.TaskStatus;
import otus.krasnyanskysa.jwtapi.security.JwtTokenProvider;
import otus.krasnyanskysa.jwtapi.service.TaskService;
import otus.krasnyanskysa.jwtapi.service.UserService;
import otus.krasnyanskysa.jwtapi.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {TaskController.class, CategoryController.class, UserController.class})
@Import(SecurityConfig.class)
class TaskControllerWebMvcTest {

    @Autowired MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean TaskService taskService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean otus.krasnyanskysa.jwtapi.service.CategoryService categoryService;
    @MockitoBean UserService userService;
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private TaskResponse sampleTask() {
        return new TaskResponse(1L, "Task 1", "desc", TaskStatus.OPEN,
                "alice", null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getTasks_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getTasks_asUser_returns200() throws Exception {
        when(taskService.findAll(anyString(), anyBoolean(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleTask())));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Task 1"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void createTask_asUser_returns201() throws Exception {
        when(taskService.create(any(), anyString())).thenReturn(sampleTask());

        mockMvc.perform(post("/api/tasks").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TaskRequest("Task 1", "desc", TaskStatus.OPEN, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Task 1"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void deleteTask_asUser_returns204() throws Exception {
        mockMvc.perform(delete("/api/tasks/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void postCategory_asUser_returns403() throws Exception {
        mockMvc.perform(post("/api/categories").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Work\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getUsers_asAdmin_returns200() throws Exception {
        when(userService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void getUsers_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void changeRole_asAdmin_returns200() throws Exception {
        when(userService.changeRole(1L, Role.ADMIN))
                .thenReturn(new UserResponse(1L, "alice", "alice@test.com", Role.ADMIN));

        mockMvc.perform(put("/api/users/1/role").with(csrf())
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void changeRole_asUser_returns403() throws Exception {
        mockMvc.perform(put("/api/users/1/role").with(csrf())
                        .param("role", "ADMIN"))
                .andExpect(status().isForbidden());
    }
}
