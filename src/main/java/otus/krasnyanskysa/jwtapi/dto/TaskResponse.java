package otus.krasnyanskysa.jwtapi.dto;

import otus.krasnyanskysa.jwtapi.entity.Task;
import otus.krasnyanskysa.jwtapi.entity.TaskStatus;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        String username,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getUser().getUsername(),
                task.getCategory() != null ? task.getCategory().getName() : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}

