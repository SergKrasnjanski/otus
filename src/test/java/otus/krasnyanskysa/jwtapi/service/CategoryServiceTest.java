package otus.krasnyanskysa.jwtapi.service;

import otus.krasnyanskysa.jwtapi.dto.CategoryRequest;
import otus.krasnyanskysa.jwtapi.dto.CategoryResponse;
import otus.krasnyanskysa.jwtapi.entity.Category;
import otus.krasnyanskysa.jwtapi.entity.User;
import otus.krasnyanskysa.jwtapi.exception.ResourceNotFoundException;
import otus.krasnyanskysa.jwtapi.repository.CategoryRepository;
import otus.krasnyanskysa.jwtapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;
    @InjectMocks CategoryService categoryService;

    @Test
    void findAll_returnsAllCategories() {
        Category cat = category("Work");
        Pageable pageable = PageRequest.of(0, 20);
        when(categoryRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(cat)));

        Page<CategoryResponse> result = categoryService.findAll(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Work", result.getContent().get(0).name());
    }

    @Test
    void findById_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category("Work")));

        CategoryResponse result = categoryService.findById(1L);

        assertEquals("Work", result.name());
    }

    @Test
    void findById_notFound_throwsException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(99L));
    }

    @Test
    void create_success() {
        var req = new CategoryRequest("Work", "Work stuff");
        User admin = new User();
        admin.setUsername("admin");
        when(categoryRepository.existsByName("Work")).thenReturn(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(categoryRepository.save(any())).thenAnswer(i -> {
            Category c = i.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse result = categoryService.create(req, "admin");

        assertEquals("Work", result.name());
    }

    @Test
    void create_duplicateName_throwsException() {
        when(categoryRepository.existsByName("Work")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> categoryService.create(new CategoryRequest("Work", null), "admin"));
    }

    @Test
    void delete_success() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category("Work")));

        categoryService.delete(1L);

        verify(categoryRepository).delete(any(Category.class));
    }

    private Category category(String name) {
        Category c = new Category();
        c.setId(1L);
        c.setName(name);
        return c;
    }
}

