package com.example.bookexchange.service;

import com.example.bookexchange.entity.User;
import com.example.bookexchange.entity.UserRole;
import com.example.bookexchange.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * BuyerService handles all buyer-related CRUD operations.
 * This service ensures proper management of buyer accounts and profiles.
 */
@Service
public class BuyerService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Create a new buyer account.
     * Encrypts the password before saving.
     *
     * @param username Unique username
     * @param email Unique email address
     * @param password Plain text password (will be encrypted)
     * @param firstName First name
     * @param lastName Last name
     * @return The created User object with BUYER role
     * @throws IllegalArgumentException if username or email already exists
     */
    @Transactional
    public User createBuyer(String username, String email, String password,
                            String firstName, String lastName) {
        // Validate inputs
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        // Check if username already exists
        if (userRepository.existsByUsername(username.trim())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new buyer user
        User buyer = new User();
        buyer.setUsername(username.trim());
        buyer.setEmail(email.trim());
        buyer.setPassword(passwordEncoder.encode(password));
        buyer.setFirstName(firstName.trim());
        buyer.setLastName(lastName.trim());
        buyer.setRole(UserRole.BUYER);
        buyer.setEnabled(true);

        return userRepository.save(buyer);
    }

    /**
     * Retrieve a buyer by ID.
     *
     * @param id The buyer's user ID
     * @return Optional containing the User if found
     * @throws IllegalArgumentException if user is not a buyer
     */
    public Optional<User> getBuyerById(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent() && !user.get().getRole().equals(UserRole.BUYER)) {
            throw new IllegalArgumentException("User is not a buyer");
        }
        return user;
    }

    /**
     * Retrieve all buyers.
     *
     * @return List of all users with BUYER role
     */
    public List<User> getAllBuyers() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(user -> user.getRole().equals(UserRole.BUYER))
                .toList();
    }

    /**
     * Update buyer information.
     * Does not allow changing username, email, or password through this method.
     *
     * @param id The buyer's user ID
     * @param firstName Updated first name
     * @param lastName Updated last name
     * @return The updated User object
     * @throws IllegalArgumentException if user not found or is not a buyer
     */
    @Transactional
    public User updateBuyer(Long id, String firstName, String lastName) {
        User buyer = getBuyerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));

        // Validate inputs
        if (firstName != null && !firstName.trim().isEmpty()) {
            buyer.setFirstName(firstName.trim());
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            buyer.setLastName(lastName.trim());
        }

        return userRepository.save(buyer);
    }

    /**
     * Update buyer password.
     *
     * @param id The buyer's user ID
     * @param newPassword New plain text password (will be encrypted)
     * @return The updated User object
     * @throws IllegalArgumentException if user not found or is not a buyer
     */
    @Transactional
    public User updateBuyerPassword(Long id, String newPassword) {
        User buyer = getBuyerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));

        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        buyer.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(buyer);
    }

    /**
     * Delete a buyer account.
     *
     * @param id The buyer's user ID
     * @throws IllegalArgumentException if user not found or is not a buyer
     */
    @Transactional
    public void deleteBuyer(Long id) {
        User buyer = getBuyerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));
        userRepository.deleteById(id);
    }

    /**
     * Enable or disable a buyer account.
     *
     * @param id The buyer's user ID
     * @param enabled True to enable, false to disable
     * @return The updated User object
     * @throws IllegalArgumentException if user not found or is not a buyer
     */
    @Transactional
    public User enableBuyer(Long id, boolean enabled) {
        User buyer = getBuyerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));
        buyer.setEnabled(enabled);
        return userRepository.save(buyer);
    }

    /**
     * Retrieve a buyer by username.
     *
     * @param username The buyer's username
     * @return Optional containing the User if found and is a buyer
     */
    public Optional<User> getBuyerByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && !user.get().getRole().equals(UserRole.BUYER)) {
            throw new IllegalArgumentException("User is not a buyer");
        }
        return user;
    }
}
