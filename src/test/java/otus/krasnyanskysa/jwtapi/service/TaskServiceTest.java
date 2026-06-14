package otus.krasnyanskysa.jwtapi.service;

import otus.krasnyanskysa.jwtapi.dto.TaskRequest;
import otus.krasnyanskysa.jwtapi.dto.TaskResponse;
import otus.krasnyanskysa.jwtapi.entity.*;
import otus.krasnyanskysa.jwtapi.exception.ForbiddenException;
import otus.krasnyanskysa.jwtapi.exception.ResourceNotFoundException;
import otus.krasnyanskysa.jwtapi.repository.CategoryRepository;
import otus.krasnyanskysa.jwtapi.repository.TaskRepository;
import otus.krasnyanskysa.jwtapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;
    @Mock CategoryRepository categoryRepository;
    @InjectMocks TaskService taskService;

    private User alice;
    private Task task;
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");

        task = new Task();
        task.setId(1L);
        task.setTitle("Test task");
        task.setStatus(TaskStatus.OPEN);
        task.setUser(alice);
    }

    @Test
    void findAll_asUser_returnsUserTasks() {
        when(taskRepository.findByUsernameAndOptionalStatus("alice", null, pageable))
                .thenReturn(new PageImpl<>(List.of(task)));

        Page<TaskResponse> result = taskService.findAll("alice", false, null, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Test task", result.getContent().get(0).title());
    }

    @Test
    void findAll_asAdmin_returnsAllTasks() {
        when(taskRepository.findAllWithUserAndCategory(pageable)).thenReturn(new PageImpl<>(List.of(task)));

        Page<TaskResponse> result = taskService.findAll("admin", true, null, pageable);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void findById_owner_success() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponse result = taskService.findById(1L, "alice", false);

        assertEquals(1L, result.id());
    }

    @Test
    void findById_notOwner_throwsForbidden() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(ForbiddenException.class, () -> taskService.findById(1L, "bob", false));
    }

    @Test
    void findById_notFound_throwsException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.findById(99L, "alice", false));
    }

    @Test
    void create_success() {
        var req = new TaskRequest("New task", "desc", TaskStatus.OPEN, null);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(taskRepository.save(any())).thenAnswer(i -> {
            Task t = i.getArgument(0);
            t.setId(2L);
            return t;
        });

        TaskResponse result = taskService.create(req, "alice");

        assertEquals("New task", result.title());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void delete_owner_success() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.delete(1L, "alice", false);

        verify(taskRepository).delete(task);
    }

    @Test
    void delete_notOwner_throwsForbidden() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(ForbiddenException.class, () -> taskService.delete(1L, "bob", false));
        verify(taskRepository, never()).delete(any());
    }
}
