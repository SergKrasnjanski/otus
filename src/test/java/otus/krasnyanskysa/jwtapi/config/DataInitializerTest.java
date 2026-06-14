package otus.krasnyanskysa.jwtapi.config;

import otus.krasnyanskysa.jwtapi.entity.Role;
import otus.krasnyanskysa.jwtapi.entity.User;
import otus.krasnyanskysa.jwtapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks DataInitializer dataInitializer;

    @BeforeEach
    void injectValues() {
        ReflectionTestUtils.setField(dataInitializer, "adminUsername", "admin");
        ReflectionTestUtils.setField(dataInitializer, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "admin123");
    }

    @Test
    void run_createsAdminIfNotExists() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin123")).thenReturn("encoded");

        dataInitializer.run(null);

        verify(userRepository).save(argThat(user ->
                Role.ADMIN.equals(user.getRole()) &&
                "admin".equals(user.getUsername()) &&
                "admin@example.com".equals(user.getEmail()) &&
                "encoded".equals(user.getPassword())));
    }

    @Test
    void run_doesNotCreateAdminIfAlreadyExists() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User()));

        dataInitializer.run(null);

        verify(userRepository, never()).save(any());
    }
}
