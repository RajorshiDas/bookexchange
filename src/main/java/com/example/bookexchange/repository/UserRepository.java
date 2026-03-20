package com.example.bookexchange.repository;

import com.example.bookexchange.entity.User;
import com.example.bookexchange.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);
    List<User> findByRole(UserRole role);
    List<User> findByRoleAndUsernameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCase(
            UserRole role, String username, UserRole role2, String email);
}
