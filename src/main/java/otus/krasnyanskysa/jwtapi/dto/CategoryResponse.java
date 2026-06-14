package otus.krasnyanskysa.jwtapi.dto;

import otus.krasnyanskysa.jwtapi.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        String description,
        String createdBy
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedBy() != null ? category.getCreatedBy().getUsername() : null
        );
    }
}

