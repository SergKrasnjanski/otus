package otus.krasnyanskysa.jwtapi.controller;

import otus.krasnyanskysa.jwtapi.dto.TaskRequest;
import otus.krasnyanskysa.jwtapi.dto.TaskResponse;
import otus.krasnyanskysa.jwtapi.entity.TaskStatus;
import otus.krasnyanskysa.jwtapi.utils.AuthUtils;
import otus.krasnyanskysa.jwtapi.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<Page<TaskResponse>> getAll(Authentication auth,
                                                     @RequestParam(required = false) TaskStatus status,
                                                     @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(taskService.findAll(auth.getName(), AuthUtils.isAdmin(auth), status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(taskService.findById(id, auth.getName(), AuthUtils.isAdmin(auth)));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request, auth.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody TaskRequest request,
                                               Authentication auth) {
        return ResponseEntity.ok(taskService.update(id, request, auth.getName(), AuthUtils.isAdmin(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        taskService.delete(id, auth.getName(), AuthUtils.isAdmin(auth));
        return ResponseEntity.noContent().build();
    }
}
