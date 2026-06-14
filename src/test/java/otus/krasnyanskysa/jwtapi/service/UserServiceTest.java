package otus.krasnyanskysa.jwtapi.service;

import otus.krasnyanskysa.jwtapi.dto.UserResponse;
import otus.krasnyanskysa.jwtapi.entity.Role;
import otus.krasnyanskysa.jwtapi.entity.User;
import otus.krasnyanskysa.jwtapi.exception.ResourceNotFoundException;
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
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    private User alice;
    private final Pageable pageable = PageRequest.of(0, 20);

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setEmail("alice@test.com");
        alice.setRole(Role.USER);
    }

    @Test
    void findAll_returnsMappedPage() {
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(alice)));

        Page<UserResponse> result = userService.findAll(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("alice", result.getContent().get(0).username());
        assertEquals(Role.USER, result.getContent().get(0).role());
    }

    @Test
    void findById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));

        UserResponse result = userService.findById(1L);

        assertEquals(1L, result.id());
        assertEquals("alice", result.username());
        assertEquals(Role.USER, result.role());
    }

    @Test
    void findById_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(99L));
    }

    @Test
    void delete_success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_notFound_throwsException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.delete(99L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void changeRole_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.save(alice)).thenReturn(alice);

        UserResponse result = userService.changeRole(1L, Role.ADMIN);

        assertEquals(Role.ADMIN, result.role());
        verify(userRepository).save(alice);
    }

    @Test
    void changeRole_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.changeRole(99L, Role.ADMIN));
        verify(userRepository, never()).save(any());
    }
}
