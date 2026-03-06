package com.example.bookexchange.config;

import com.example.bookexchange.entity.User;
import com.example.bookexchange.entity.UserRole;
import com.example.bookexchange.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // Check if admin user already exists — do nothing if it does
        if (userRepository.existsByUsername("admin")) {
            System.out.println("✅ Admin user already exists. Skipping creation.");
            return;
        }

        // Create the default admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@bookexchange.com");
        admin.setPassword(passwordEncoder.encode("admin123")); // BCrypt hash
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);

        userRepository.save(admin);

        System.out.println("✅ Default admin user created successfully.");
        System.out.println("   Username : admin");
        System.out.println("   Password : admin123");
        System.out.println("   Role     : ADMIN");
    }
}

