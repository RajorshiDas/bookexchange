package com.example.bookexchange.controller;

import com.example.bookexchange.dto.RegisterRequest;
import com.example.bookexchange.entity.UserRole;
import com.example.bookexchange.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    // Constructor injection
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ----------------------------------------------------------------
    // GET /auth/login — show login page only
    // POST /auth/login is handled entirely by Spring Security
    // ----------------------------------------------------------------
    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    // ----------------------------------------------------------------
    // GET /auth/register — show registration form
    // ----------------------------------------------------------------
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        // Only pass BUYER and SELLER — ADMIN cannot self-register
        model.addAttribute("roles", List.of(UserRole.BUYER, UserRole.SELLER));

        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    // ----------------------------------------------------------------
    // POST /auth/register — register a new user via AuthService
    // ----------------------------------------------------------------
    @PostMapping("/register")
    public String handleRegister(@ModelAttribute RegisterRequest registerRequest,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        try {
            authService.register(registerRequest);
            // Success: flash message and redirect to login
            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Please login.");
            return "redirect:/auth/login";

        } catch (Exception e) {
            // Error: return to register page with error and preserve form data
            model.addAttribute("error", e.getMessage());
            model.addAttribute("roles", List.of(UserRole.BUYER, UserRole.SELLER));
            model.addAttribute("registerRequest", registerRequest);
            return "register";
        }
    }
}
