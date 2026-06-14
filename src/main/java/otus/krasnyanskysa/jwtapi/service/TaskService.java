package otus.krasnyanskysa.jwtapi.service;

import otus.krasnyanskysa.jwtapi.dto.TaskRequest;
import otus.krasnyanskysa.jwtapi.dto.TaskResponse;
import otus.krasnyanskysa.jwtapi.entity.Task;
import otus.krasnyanskysa.jwtapi.entity.TaskStatus;
import otus.krasnyanskysa.jwtapi.entity.User;
import otus.krasnyanskysa.jwtapi.exception.ForbiddenException;
import otus.krasnyanskysa.jwtapi.exception.ResourceNotFoundException;
import otus.krasnyanskysa.jwtapi.repository.CategoryRepository;
import otus.krasnyanskysa.jwtapi.repository.TaskRepository;
import otus.krasnyanskysa.jwtapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<TaskResponse> findAll(String username, boolean isAdmin, TaskStatus status, Pageable pageable) {
        if (isAdmin) {
            return taskRepository.findAllWithUserAndCategory(pageable).map(TaskResponse::from);
        }
        return taskRepository.findByUsernameAndOptionalStatus(username, status, pageable)
                .map(TaskResponse::from);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id, String username, boolean isAdmin) {
        Task task = getTaskOrThrow(id);
        if (!isAdmin && !task.getUser().getUsername().equals(username)) {
            throw new ForbiddenException("Access denied to this task");
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse create(TaskRequest request, String username) {
        User user = getUserOrThrow(username);
        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.OPEN);
        task.setUser(user);
        if (request.categoryId() != null) {
            task.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId())));
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request, String username, boolean isAdmin) {
        Task task = getTaskOrThrow(id);
        if (!isAdmin && !task.getUser().getUsername().equals(username)) {
            throw new ForbiddenException("Access denied to this task");
        }
        task.setTitle(request.title());
        task.setDescription(request.description());
        if (request.status() != null) task.setStatus(request.status());
        if (request.categoryId() != null) {
            task.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId())));
        } else {
            task.setCategory(null);
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id, String username, boolean isAdmin) {
        Task task = getTaskOrThrow(id);
        if (!isAdmin && !task.getUser().getUsername().equals(username)) {
            throw new ForbiddenException("Access denied to this task");
        }
        taskRepository.delete(task);
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
