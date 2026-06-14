package otus.krasnyanskysa.jwtapi.dto;

import otus.krasnyanskysa.jwtapi.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;

public record TaskRequest(
        @NotBlank(message = "Task title is required") String title,
        String description,
        TaskStatus status,
        Long categoryId
) {}

