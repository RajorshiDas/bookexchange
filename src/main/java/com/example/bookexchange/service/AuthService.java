package com.example.bookexchange.service;

import com.example.bookexchange.dto.LoginRequest;
import com.example.bookexchange.dto.RegisterRequest;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.entity.UserRole;
import com.example.bookexchange.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String register(RegisterRequest request) throws Exception {

        // --- Step 1: Validate required fields ---
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new Exception("Username is required");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new Exception("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new Exception("Password is required");
        }
        if (request.getConfirmPassword() == null || request.getConfirmPassword().isEmpty()) {
            throw new Exception("Please confirm your password");
        }
        if (request.getRole() == null) {
            throw new Exception("Role is required");
        }

        // --- Step 2: Passwords must match ---
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new Exception("Passwords do not match");
        }

        // --- Step 3: Block ADMIN self-registration ---
        // Only BUYER and SELLER are allowed to register publicly
        if (request.getRole() == UserRole.ADMIN) {
            throw new Exception("Admin accounts cannot be created through public registration");
        }

        // Only BUYER or SELLER are valid roles
        if (request.getRole() != UserRole.BUYER && request.getRole() != UserRole.SELLER) {
            throw new Exception("Invalid role. Please select Buyer or Seller");
        }

        // --- Step 4: Check for duplicate username ---
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new Exception("Username '" + request.getUsername() + "' is already taken");
        }

        // --- Step 5: Check for duplicate email ---
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new Exception("An account with email '" + request.getEmail() + "' already exists");
        }

        // --- Step 6: Build and save the new user ---
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // BCrypt hash
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());
        user.setEnabled(true); // Account is active immediately

        userRepository.save(user);

        return "Registration successful! Welcome, " + user.getFirstName()
                + ". You are registered as a " + user.getRole().name() + ".";
    }

    public Optional<User> authenticate(LoginRequest request) {
        Optional<User> user = userRepository.findByUsername(request.getUsername());
        if (user.isPresent() && passwordEncoder.matches(request.getPassword(), user.get().getPassword())) {
            return user;
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}

