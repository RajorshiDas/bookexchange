package com.example.bookexchange.controller.api;

import com.example.bookexchange.dto.LoginRequest;
import com.example.bookexchange.dto.RegisterRequest;
import com.example.bookexchange.entity.User;
import com.example.bookexchange.exception.BadRequestException;
import com.example.bookexchange.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final AuthService authService;

    public AuthRestController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful");
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        User user = authService.authenticate(request)
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole());
        return ResponseEntity.ok(response);
    }
}
