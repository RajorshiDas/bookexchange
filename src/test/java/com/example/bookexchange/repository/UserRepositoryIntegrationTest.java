package com.example.bookexchange.repository;

import com.example.bookexchange.entity.User;
import com.example.bookexchange.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryIntegrationTest {

    @Autowired
    private ExchangeRequestRepository exchangeRequestRepository;

    @Autowired
    private BookListingRepository bookListingRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        exchangeRequestRepository.deleteAll();
        bookListingRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindUserByUsername() {
        String username = uniqueValue("buyer");
        String email = uniqueValue("buyer") + "@example.com";
        User user = buildUser(username, email, UserRole.BUYER);
        userRepository.save(user);

        Optional<User> result = userRepository.findByUsername(username);

        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        assertEquals(email, result.get().getEmail());
        assertEquals(UserRole.BUYER, result.get().getRole());
    }

    @Test
    void shouldReturnTrueWhenUsernameExists() {
        String username = uniqueValue("seller");
        userRepository.save(buildUser(username, uniqueValue("seller") + "@example.com", UserRole.SELLER));

        boolean exists = userRepository.existsByUsername(username);

        assertTrue(exists);
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        String email = uniqueValue("buyer") + "@example.com";
        userRepository.save(buildUser(uniqueValue("buyer"), email, UserRole.BUYER));

        boolean exists = userRepository.existsByEmail(email);

        assertTrue(exists);
    }

    @Test
    void shouldReturnEmptyWhenUsernameDoesNotExist() {
        Optional<User> result = userRepository.findByUsername(uniqueValue("missing"));

        assertFalse(result.isPresent());
    }

    private String uniqueValue(String prefix) {
        return prefix + "_" + UUID.randomUUID();
    }

    private User buildUser(String username, String email, UserRole role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
