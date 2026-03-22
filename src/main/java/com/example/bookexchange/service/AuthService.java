package com.example.bookexchange.service;

import com.example.bookexchange.dto.LoginRequest;
import com.example.bookexchange.dto.RegisterRequest;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.entity.UserRole;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.exception.ForbiddenOperationException;
import com.example.bookexchange.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User register(RegisterRequest request) {

        if (request == null) {
            logger.warn("Register validation failed: request is null");
            throw new BadRequestException("Registration request is required");
        }

        String username = request.getUsername();
        String email = request.getEmail();
        logger.info("Register attempt username={}, email={}", username, email);

        // --- Step 1: Validate required fields ---
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Register validation failed: username is required");
            throw new BadRequestException("Username is required");
        }
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Register validation failed: email is required for username={}", username);
            throw new BadRequestException("Email is required");
        }
        String password = request.getPassword();
        if (password == null || password.trim().isEmpty()) {
            logger.warn("Register validation failed: password is required for username={}", username);
            throw new BadRequestException("Password is required");
        }
        String confirmPassword = request.getConfirmPassword();
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            logger.warn("Register validation failed: confirm password is required for username={}", username);
            throw new BadRequestException("Please confirm your password");
        }
        String firstName = request.getFirstName();
        if (firstName == null || firstName.trim().isEmpty()) {
            logger.warn("Register validation failed: first name is required for username={}", username);
            throw new BadRequestException("First name is required");
        }
        String lastName = request.getLastName();
        if (lastName == null || lastName.trim().isEmpty()) {
            logger.warn("Register validation failed: last name is required for username={}", username);
            throw new BadRequestException("Last name is required");
        }
        if (request.getRole() == null) {
            logger.warn("Register validation failed: role is required for username={}", username);
            throw new BadRequestException("Role is required");
        }

        String trimmedPassword = password.trim();
        String trimmedConfirmPassword = confirmPassword.trim();

        // --- Step 2: Passwords must match ---
        if (!trimmedPassword.equals(trimmedConfirmPassword)) {
            logger.warn("Register validation failed: password mismatch for username={}", username);
            throw new BadRequestException("Passwords do not match");
        }

        // --- Step 3: Block ADMIN self-registration ---
        // Only BUYER and SELLER are allowed to register publicly
        if (request.getRole() == UserRole.ADMIN) {
            logger.warn("Register validation failed: admin role not allowed for username={}", username);
            throw new ForbiddenOperationException("Admin accounts cannot be created through public registration");
        }

        // Only BUYER or SELLER are valid roles
        if (request.getRole() != UserRole.BUYER && request.getRole() != UserRole.SELLER) {
            logger.warn("Register validation failed: invalid role for username={}", username);
            throw new BadRequestException("Invalid role. Please select Buyer or Seller");
        }

        String trimmedUsername = username.trim();
        String trimmedEmail = email.trim();
        String trimmedFirstName = firstName.trim();
        String trimmedLastName = lastName.trim();

        // --- Step 4: Check for duplicate username ---
        if (userRepository.existsByUsername(trimmedUsername)) {
            logger.warn("Register validation failed: username already exists for username={}", trimmedUsername);
            throw new BadRequestException("Username '" + trimmedUsername + "' is already taken");
        }

        // --- Step 5: Check for duplicate email ---
        if (userRepository.existsByEmail(trimmedEmail)) {
            logger.warn("Register validation failed: email already exists for email={}", trimmedEmail);
            throw new BadRequestException("An account with email '" + trimmedEmail + "' already exists");
        }

        // --- Step 6: Build and save the new user ---
        User user = new User();
        user.setUsername(trimmedUsername);
        user.setEmail(trimmedEmail);
        user.setPassword(passwordEncoder.encode(trimmedPassword)); // BCrypt hash
        user.setFirstName(trimmedFirstName);
        user.setLastName(trimmedLastName);
        user.setRole(request.getRole());
        user.setEnabled(true); // Account is active immediately

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: id={}, username={}", savedUser.getId(), savedUser.getUsername());

        return savedUser;
    }

    public Optional<User> authenticate(LoginRequest request) {
        if (request == null) {
            logger.warn("Login validation failed: request is null");
            throw new BadRequestException("Login request is required");
        }
        String username = request.getUsername();
        logger.info("Login attempt username={}", username);
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Login validation failed: username is required");
            throw new BadRequestException("Username is required");
        }
        String password = request.getPassword();
        if (password == null || password.trim().isEmpty()) {
            logger.warn("Login validation failed: password is required for username={}", username);
            throw new BadRequestException("Password is required");
        }
        String trimmedUsername = username.trim();
        String trimmedPassword = password.trim();
        Optional<User> user = userRepository.findByUsername(trimmedUsername);
        if (user.isEmpty()) {
            logger.warn("Login failed: user not found for username={}", trimmedUsername);
            throw new BadRequestException("Invalid username or password");
        }
        if (!passwordEncoder.matches(trimmedPassword, user.get().getPassword())) {
            logger.warn("Login failed: invalid password for username={}", trimmedUsername);
            throw new BadRequestException("Invalid username or password");
        }
        logger.info("Login success: id={}, username={}", user.get().getId(), user.get().getUsername());
        return user;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
