package com.example.bookexchange.service;

import com.example.bookexchange.dto.RegisterRequest;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.entity.UserRole;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.ForbiddenOperationException;
import com.example.bookexchange.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void register_succeeds_for_buyer() {
        RegisterRequest request = validRequest();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("hashedSecret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });

        User savedUser = authService.register(request);

        assertNotNull(savedUser);
        assertEquals(42L, savedUser.getId());
        assertEquals("alice", savedUser.getUsername());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals("hashedSecret", savedUser.getPassword());
        assertEquals(UserRole.BUYER, savedUser.getRole());
        assertEquals(true, savedUser.getEnabled());

        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("alice", capturedUser.getUsername());
        assertEquals("alice@example.com", capturedUser.getEmail());
        assertEquals("hashedSecret", capturedUser.getPassword());
    }

    @Test
    void register_fails_when_request_is_null() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(null)
        );

        assertEquals("Registration request is required", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_fails_when_username_is_missing() {
        RegisterRequest request = validRequest();
        request.setUsername(" ");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertEquals("Username is required", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_fails_when_passwords_do_not_match() {
        RegisterRequest request = validRequest();
        request.setConfirmPassword("Different");

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertEquals("Passwords do not match", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_blocks_admin_role() {
        RegisterRequest request = validRequest();
        request.setRole(UserRole.ADMIN);

        ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> authService.register(request)
        );

        assertEquals("Admin accounts cannot be created through public registration", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_fails_when_username_is_taken() {
        RegisterRequest request = validRequest();

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertEquals("Username 'alice' is already taken", exception.getMessage());
        verify(userRepository, never()).existsByEmail(any(String.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_fails_when_email_is_taken() {
        RegisterRequest request = validRequest();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertEquals("An account with email 'alice@example.com' already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    private RegisterRequest validRequest() {
        return new RegisterRequest(
                "alice",
                "alice@example.com",
                "Secret123",
                "Secret123",
                "Alice",
                "Smith",
                UserRole.BUYER
        );
    }
}

